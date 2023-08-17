package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URL
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

internal class SqsPoller(
    private val sqs: SqsAsyncClient,
    private val executor: SqsProducerExecutor
) {
    private var consume: Boolean = false
    suspend fun poll(consumers: List<SqsConsumer>) = coroutineScope {
        consume = true
        // for each consumer, spawn a new coroutine
        consumers.forEach {
            launch {
                while (consume && it.config.consumeWhile()) {
                    accept(it)
                    delay(it.config.sleepTime.toMillis())
                }
            }
        }
    }

    fun stopPolling() {
        consume = false
    }

    private suspend fun accept(consumer: SqsConsumer) = coroutineScope {
        repeat(consumer.config.concurrency) {
            launch {
                retry(
                    consumer.config.retries,
                    {
                        when (it) {
                            is AwsServiceException ->
                                consumer.onError(
                                    ConsumptionError.AwsError(details = it.awsErrorDetails())
                                )
                            is SdkClientException ->
                                consumer.onError(ConsumptionError.AwsSdkClientError(it))
                            else -> consumer.onError(ConsumptionError.UnrecognizedError(it))
                        }
                    }
                ) {
                    val request =
                        ReceiveMessageRequest.builder()
                            .queueUrl(consumer.queueUri.toString())
                            .maxNumberOfMessages(consumer.config.maxMessages)
                            .waitTimeSeconds(consumer.config.waitTime.toSecondsPart())
                            .build()

                    sqs.receiveMessage(request).await().let { response ->
                        val messages = response.messages()

                        if (messages.isNotEmpty()) {
                            val pipeline = consumer.pipeline

                            // consume the messages in parallel
                            messages.forEach { message ->
                                launch {
                                    val consumed =
                                        pipeline?.producer?.let {
                                            executor.produce(it) { pipeline.transformer(message) }
                                            // consumption worked, so we specify it manually
                                            // instead of having the pipeline handle it
                                            true
                                        }
                                            ?: consumer.onSuccess(message)

                                    if (consumed) {
                                        // we acknowledge the message only if the consumption
                                        // succeeded
                                        acknowledge(messages, consumer.queueUri)
                                    } else {
                                        // otherwise, we use the error handler of the consumer
                                        consumer.onError(
                                            ConsumptionError.UnsuccessfulConsumption(message)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun acknowledge(messages: List<Message>, queueUrl: URL) {
        val entries =
            messages.map {
                DeleteMessageBatchRequestEntry.builder()
                    .id(it.messageId())
                    .receiptHandle(it.receiptHandle())
                    .build()
            }

        val request =
            DeleteMessageBatchRequest.builder()
                .queueUrl(queueUrl.toString())
                .entries(entries)
                .build()

        sqs.deleteMessageBatch(request).await()
    }
}
