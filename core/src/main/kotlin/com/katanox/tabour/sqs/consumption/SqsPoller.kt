package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsPipeline
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URI
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.awscore.exception.AwsServiceException
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
                            else -> consumer.onError(ConsumptionError.UnrecognizedError(it))
                        }
                    }
                ) {
                    val request =
                        ReceiveMessageRequest.builder()
                            .queueUrl(consumer.queueUrl.toASCIIString())
                            .maxNumberOfMessages(consumer.config.maxMessages)
                            .waitTimeSeconds(consumer.config.waitTime.toSecondsPart())
                            .build()

                    sqs.receiveMessage(request).await().let { response ->
                        val messages = response.messages()

                        if (messages.isNotEmpty()) {
                            val pipeline = consumer.pipeline

                            if (pipeline != null && isPipelineSet(pipeline)) {
                                messages.forEach { message ->
                                    launch {
                                        executor.produce(pipeline.producer) {
                                            pipeline.prodFn(message)
                                        }
                                    }
                                }
                            } else {
                                messages.forEach { launch { consumer.onSuccess(it) } }
                            }

                            launch { acknowledge(messages, consumer.queueUrl) }
                        }
                    }
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

private fun isPipelineSet(pipeline: SqsPipeline): Boolean =
    pipeline.prodFnWasSet && pipeline.producerWasSet
