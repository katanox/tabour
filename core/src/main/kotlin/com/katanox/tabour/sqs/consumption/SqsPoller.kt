package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.configuration.core.DataProductionConfiguration
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.production.SqsDataForProduction
import com.katanox.tabour.sqs.production.SqsMessageProduced
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onSuccess
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
        val startedConsumerIndexes = Array(consumers.size) { false }
        val jobIndexes: Array<Job?> = Array(consumers.size) { null }

        launch { startAcknowledging() }

        while (consume) {
            consumers.forEachIndexed { index, consumer ->
                if (!startedConsumerIndexes[index] && consumer.config.consumeWhile()) {
                    val job = launch {
                        accept(consumer)
                        delay(consumer.config.sleepTime.toMillis())
                    }

                    startedConsumerIndexes[index] = true
                    jobIndexes[index] = job
                } else if (startedConsumerIndexes[index] && !consumer.config.consumeWhile()) {
                    jobIndexes[index]?.cancelAndJoin()

                    startedConsumerIndexes[index] = false
                    jobIndexes[index] = null
                }
            }

            if (startedConsumerIndexes.none { !it }) {
                consume = false
            }

            delay(1000)
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
                        val error =
                            when (it) {
                                is AwsServiceException ->
                                    ConsumptionError.AwsError(details = it.awsErrorDetails())
                                is SdkClientException -> ConsumptionError.AwsSdkClientError(it)
                                else -> ConsumptionError.UnrecognizedError(it)
                            }

                        consumer.onError(error)
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
                        toAcknowledge.trySend(ToBeAcknowledged(consumer.queueUri, message))
                        consumer.notifyPlugs(message)
                    } else {
                        val error = ConsumptionError.UnsuccessfulConsumption(message)
                        consumer.onError(error)
                        consumer.notifyPlugs(message, error)
                    }
                }
            }
        }

    private suspend fun SqsConsumer.notifyPlugs(message: Message, error: ConsumptionError? = null) {
        if (this.plugs.isNotEmpty()) {
            this.plugs.forEach { plug ->
                if (error == null) {
                    plug.onSuccess(message)
                } else {
                    plug.onFailure(message, error)
                }
            }
        }
    }

    private suspend fun startAcknowledging() {
        while (consume) {
            buildList {
                    repeat(10) {
                        val result = toAcknowledge.tryReceive()
                        result.onSuccess { this.add(it) }
                    }
                }
                .groupBy(ToBeAcknowledged::url)
                .forEach { (url, messages) ->
                    val entries =
                        messages.map {
                            DeleteMessageBatchRequestEntry.builder()
                                .id(it.message.messageId())
                                .receiptHandle(it.message.receiptHandle())
                                .build()
                        }

                    if (entries.isNotEmpty()) {
                        val request =
                            DeleteMessageBatchRequest.builder()
                                .queueUrl(url.toString())
                                .entries(entries)
                                .build()

                        sqs.deleteMessageBatch(request)
                    }
                }

            delay(100)
        }
    }
}
