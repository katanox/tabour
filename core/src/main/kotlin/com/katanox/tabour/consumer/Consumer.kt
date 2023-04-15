package com.katanox.tabour.consumer

import com.katanox.tabour.config.SqsConfiguration
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@OptIn(DelicateCoroutinesApi::class)
val scope = CoroutineScope(context = newFixedThreadPoolContext(16, "sqs-poller"))

class SqsConsumer(credentialsProvider: AwsCredentialsProvider, region: Region) {
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder().credentialsProvider(credentialsProvider).region(region).build()

    suspend fun start(consumers: List<SqsConfiguration>) {
        consumers.forEach { scope.launch { accept(it) } }
    }

    private suspend fun accept(configuration: SqsConfiguration) {
        while (true) {
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
                            response.messages().forEach { configuration.successFn(it) }

                            acknowledge(response.messages(), configuration.queueUrl)
                        }
                    } catch (e: Exception) {
                        configuration.onFail(e)
                    }
                }
            }

            delay(configuration.sleepTime.toMillis())
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
