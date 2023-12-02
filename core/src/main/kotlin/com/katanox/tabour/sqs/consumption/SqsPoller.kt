package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.configuration.core.DataProductionConfiguration
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.production.SqsDataForProduction
import com.katanox.tabour.sqs.production.SqsMessageProduced
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URL
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

private data class ToBeAcknowledged(val url: URL, val message: Message)

internal class SqsPoller(private val sqs: SqsClient, private val executor: SqsProducerExecutor) {
    private var consume: Boolean = false
    private val toAcknowledge = Channel<ToBeAcknowledged>()

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

        launch { startAcknowledger() }
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

                    val messages = sqs.receiveMessage(request).messages()

                    if (messages.isNotEmpty()) {
                        handleMessages(messages, consumer)
                    }
                }
            }
        }
    }

    private suspend fun handleMessages(messages: List<Message>, consumer: SqsConsumer) =
        coroutineScope {
            val pipeline = consumer.pipeline

            messages.forEach { message ->
                launch {
                    val consumed =
                        pipeline?.producer?.let {
                            var consumedFromPipeline = false

                            val produceData: () -> SqsDataForProduction = {
                                pipeline.transformer(message).also { transformationResult ->
                                    consumedFromPipeline =
                                        !transformationResult.message.isNullOrEmpty()
                                }
                            }

                            val messageProduced:
                                (SqsDataForProduction, SqsMessageProduced) -> Unit =
                                { _, _ ->
                                }
                            val failedToProduceData = pipeline.failedHandler

                            executor.produce(
                                it,
                                DataProductionConfiguration(
                                    produceData,
                                    messageProduced,
                                    failedToProduceData
                                )
                            )

                            consumedFromPipeline
                        }
                            ?: consumer.onSuccess(message)

                    if (consumed) {
                        toAcknowledge.send(ToBeAcknowledged(consumer.queueUri, message))
                    } else {
                        // otherwise, we use the error handler of the consumer
                        consumer.onError(ConsumptionError.UnsuccessfulConsumption(message))
                    }
                }
            }
        }

    private suspend fun startAcknowledger() {
        while (consume) {
            buildList { repeat(10) { this.add(toAcknowledge.receive()) } }
                .groupBy(ToBeAcknowledged::url)
                .forEach { (url, messages) ->
                    val entries =
                        messages.map {
                            DeleteMessageBatchRequestEntry.builder()
                                .id(it.message.messageId())
                                .receiptHandle(it.message.receiptHandle())
                                .build()
                        }

                    val request =
                        DeleteMessageBatchRequest.builder()
                            .queueUrl(url.toString())
                            .entries(entries)
                            .build()

                    sqs.deleteMessageBatch(request)
                }

            delay(5_000)
        }
    }
}
