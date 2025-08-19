package com.katanox.tabour.sqs.consumption

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.Message
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.katanox.tabour.TABOUR_SHUTDOWN_MESSAGE
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException

internal class SqsPoller(private val sqsClient: SqsClient) {
    private var consume: Boolean = false
    private var jobs: Array<Job?> = arrayOf()
    private val logger = KotlinLogging.logger {}

    suspend fun poll(consumers: List<SqsConsumer<*>>) = coroutineScope {
        consume = true
        val startedConsumerIndexes = Array(consumers.size) { false }
        val jobIndexes: Array<Job?> = Array(consumers.size) { null }

        launch {
            while (consume) {
                consumers.forEachIndexed { index, consumer ->
                    if (!startedConsumerIndexes[index] && consumer.config.consumeWhile()) {
                        val job = launch {
                            while (true) {
                                accept(consumer)
                                delay(consumer.config.sleepTime)
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
                }

                delay(5000)
            }
        }

        jobs = jobIndexes
    }

    suspend fun stopPolling() {
        consume = false
        jobs.forEach { it?.cancelAndJoin() }
    }

    private suspend fun <T> accept(consumer: SqsConsumer<T>) = coroutineScope {
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
                    val request = ReceiveMessageRequest {
                        queueUrl = consumer.queueUri.toString()
                        maxNumberOfMessages = consumer.config.maxMessages
                        waitTimeSeconds = consumer.config.waitTime.inWholeSeconds.toInt()
                    }

                    val messages = sqsClient.receiveMessage(request).messages ?: emptyList()

                    if (messages.isNotEmpty()) {
                        handleMessages(messages, consumer)
                    }
                }
            }
        }
    }

    private suspend fun <T> handleMessages(messages: List<Message>, consumer: SqsConsumer<T>) {
        val processedMessages =
            messages.mapNotNull { message ->
                try {
                    if (consumer.onSuccess(message)) {
                        message
                    } else {
                        val error = ConsumptionError.UnsuccessfulConsumption(message)
                        consumer.onError(error)
                        null
                    }
                } catch (e: Throwable) {
                    if (e.message != TABOUR_SHUTDOWN_MESSAGE) {
                        consumer.onError(ConsumptionError.ThrowableDuringHanding(e))
                    }
                    null
                }
            }

        acknowledge(consumer.queueUri, processedMessages)
    }

    private suspend fun acknowledge(url: URL, messages: List<Message>) {
        if (messages.isNotEmpty()) {
            try {
                val entries =
                    messages
                        .distinctBy { it.messageId }
                        .map {
                            DeleteMessageBatchRequestEntry {
                                id = it.messageId
                                receiptHandle = it.receiptHandle
                            }
                        }

                val request = DeleteMessageBatchRequest {
                    queueUrl = url.toString()
                    this.entries = entries
                }

                val deleteResponse = sqsClient.deleteMessageBatch(request)

                if (deleteResponse.failed.isNotEmpty()) {
                    val failedMessages = deleteResponse.failed.map { it.message }

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
