package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.TABOUR_SHUTDOWN_MESSAGE
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import kotlin.time.Duration
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

private data class ToBeAcknowledged(val url: URL, val message: Message)

private data class AcknowledgeConfiguration(
    val channel: Channel<ToBeAcknowledged>,
    val acknowledgeTime: Duration,
)

private val logger = KotlinLogging.logger {}

internal class SqsPoller(private val sqs: SqsClient) {
    private var consume: Boolean = false
    private var acknowledge: Boolean = true
    private var jobs: Array<Job?> = arrayOf()
    private val acknowledgeChannels: MutableMap<Int, AcknowledgeConfiguration> = mutableMapOf()

    suspend fun poll(consumers: List<SqsConsumer<*>>) = coroutineScope {
        consume = true
        val startedConsumerIndexes = Array(consumers.size) { false }
        val jobIndexes: Array<Job?> = Array(consumers.size) { null }

        launch {
            acknowledge = true
            startAcknowledging()
        }

        launch {
            while (consume) {
                consumers.forEachIndexed { index, consumer ->
                    if (!startedConsumerIndexes[index] && consumer.config.consumeWhile()) {
                        val job = launch {
                            while (true) {
                                acknowledgeChannels[index] =
                                    AcknowledgeConfiguration(
                                        Channel(),
                                        consumer.config.acknowledgeTime,
                                    )
                                accept(consumer, index)
                                delay(consumer.config.sleepTime.inWholeMilliseconds)
                            }
                        }

                        startedConsumerIndexes[index] = true
                        jobIndexes[index] = job
                    } else if (startedConsumerIndexes[index] && !consumer.config.consumeWhile()) {
                        jobIndexes[index]?.cancelAndJoin()

                        startedConsumerIndexes[index] = false
                        jobIndexes[index] = null
                    }
                }

                if (startedConsumerIndexes.none { it }) {
                    consume = false
                    acknowledge = false
                }

                delay(5000)
            }
        }
        jobs = jobIndexes
    }

    suspend fun stopPolling() {
        consume = false
        acknowledge = false
        acknowledgeChannels.clear()
        jobs.forEach {
            it?.cancelAndJoin()
            yield()
        }
    }

    private suspend fun <T> accept(consumer: SqsConsumer<T>, index: Int) = coroutineScope {
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
                    },
                ) {
                    val request =
                        ReceiveMessageRequest.builder()
                            .queueUrl(consumer.queueUri.toString())
                            .maxNumberOfMessages(consumer.config.maxMessages)
                            .waitTimeSeconds(consumer.config.waitTime.inWholeSeconds.toInt())
                            .build()

                    val messages = sqs.receiveMessage(request).messages()

                    if (messages.isNotEmpty()) {
                        logger.debug { "Received $messages from ${consumer.queueUri}" }
                        handleMessages(messages, consumer, index)
                    }
                }
            }
        }
    }

    private suspend fun <T> handleMessages(
        messages: List<Message>,
        consumer: SqsConsumer<T>,
        consumerIndex: Int,
    ) {
        messages.forEach { message ->
            try {
                if (consumer.onSuccess(message)) {
                    acknowledgeChannels[consumerIndex]
                        ?.channel
                        ?.send(ToBeAcknowledged(consumer.queueUri, message))
                } else {
                    val error = ConsumptionError.UnsuccessfulConsumption(message)
                    consumer.onError(error)
                }
            } catch (e: Throwable) {
                if (e.message != TABOUR_SHUTDOWN_MESSAGE) {
                    consumer.onError(ConsumptionError.ThrowableDuringHanding(e))
                }
            }
        }
    }

    private suspend fun startAcknowledging() = coroutineScope {
        acknowledgeChannels.forEach { (_, acknowledgeConfiguration) ->
            launch {
                while (acknowledge) {
                    launch {
                        buildList {
                                repeat(10) {
                                    val element =
                                        acknowledgeConfiguration.channel.tryReceive().getOrNull()
                                    if (element != null) {
                                        add(element)
                                    }
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
                                    try {
                                        val request =
                                            DeleteMessageBatchRequest.builder()
                                                .queueUrl(url.toString())
                                                .entries(entries)
                                                .build()

                                        val deleteResponse = sqs.deleteMessageBatch(request)

                                        if (deleteResponse.hasFailed()) {
                                            val failedMessages =
                                                deleteResponse.failed().map { it.message() }

                                            logger.error {
                                                "There are failures while deleting batch. ${failedMessages.joinToString(", ")}"
                                            }
                                        } else {
                                            logger.debug { "Successfully deleted batch" }
                                        }
                                    } catch (e: Throwable) {
                                        logger.error(e) { "Failed to delete message batch" }
                                    }
                                }
                            }
                    }
                    delay(acknowledgeConfiguration.acknowledgeTime)
                }
            }
        }
    }
}
