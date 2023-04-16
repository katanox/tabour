package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.ConsumptionError
import com.katanox.tabour.sqs.config.SqsQueueConfiguration
import java.net.URI
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.awscore.exception.AwsServiceException
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

    suspend fun poll(consumers: List<SqsQueueConfiguration>) {
        consumers.forEach {
            scope.launch {
                while (true) {
                    accept(it)
                    delay(it.config.sleepTime.toMillis())
                }
            }
        }
    }

    suspend fun accept(configuration: SqsQueueConfiguration) {
        repeat(configuration.config.concurrency) {
            scope.launch {
                val request =
                    ReceiveMessageRequest.builder()
                        .queueUrl(configuration.queueUrl.toASCIIString())
                        .maxNumberOfMessages(configuration.config.maxMessages)
                        .build()

                try {
                    sqs.receiveMessage(request).await().let { response ->
                        val messages = response.messages()

                        if (messages.isNotEmpty()) {
                            messages.forEach { configuration.onSuccess(it) }
                            acknowledge(messages, configuration.queueUrl)
                        }
                    }
                } catch (e: AwsServiceException) {
                    configuration.onError(ConsumptionError.AwsError(details = e.awsErrorDetails()))
                }
            }
        }
    }

    private suspend fun acknowledge(messages: List<Message>, queueUrl: URI) {
        val entries =
            messages.map {
                DeleteMessageBatchRequestEntry.builder()
                    .id(it.messageId())
                    .receiptHandle(it.receiptHandle())
                    .build()
            }

        val request =
            DeleteMessageBatchRequest.builder()
                .queueUrl(queueUrl.toASCIIString())
                .entries(entries)
                .build()

        sqs.deleteMessageBatch(request).await()
    }
}
