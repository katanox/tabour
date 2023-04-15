package com.katanox.tabour.consumer

import com.katanox.tabour.config.SqsConfiguration
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@OptIn(DelicateCoroutinesApi::class)
internal class SqsPoller(
    private val sqs: SqsAsyncClient,
    context: CoroutineDispatcher = newFixedThreadPoolContext(16, "sqs-poller")
) {
    private val scope = CoroutineScope(context)

    suspend fun poll(consumers: List<SqsConfiguration>) {
        consumers.forEach {
            scope.launch {
                while (true) {
                    accept(it)
                    delay(it.sleepTime.toMillis())
                }
            }
        }
    }

    suspend fun accept(configuration: SqsConfiguration) {
        repeat(configuration.workers) {
            scope.launch {
                val request =
                    ReceiveMessageRequest.builder()
                        .queueUrl(configuration.queueUrl)
                        .maxNumberOfMessages(configuration.maxMessages)
                        .waitTimeSeconds(configuration.waitTime.toSecondsPart())
                        .build()

                try {
                    sqs.receiveMessage(request).await().let { response ->
                        val messages = response.messages()

                        if (messages.isNotEmpty()) {
                            messages.forEach { configuration.successFn(it) }
                            acknowledge(messages, configuration.queueUrl)
                        }
                    }
                } catch (e: Exception) {
                    configuration.errorFn(e)
                }
            }
        }
    }

    private suspend fun acknowledge(messages: List<Message>, queueUrl: String) {
        val entries =
            messages.map {
                DeleteMessageBatchRequestEntry.builder()
                    .id(it.messageId())
                    .receiptHandle(it.receiptHandle())
                    .build()
            }

        val request =
            DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(entries).build()

        sqs.deleteMessageBatch(request).await()
    }
}
