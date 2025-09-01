package com.katanox.tabour.sqs.consumption

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.DeleteMessageRequest
import aws.sdk.kotlin.services.sqs.model.Message
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.smithy.kotlin.runtime.ServiceException
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.config.SqsConsumer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal class SqsPoller(private val sqsClient: SqsClient) {
    private var consume: Boolean = false
    private var jobs: Array<Job?> = arrayOf()
    private val logger = KotlinLogging.logger {}
    private val consumersActivityCheckTimeout = 5.seconds

    suspend fun poll(consumers: List<SqsConsumer<*>>) = coroutineScope {
        consume = true
        val startedConsumerIndexes = Array(consumers.size) { false }
        val jobIndexes: Array<Job?> = Array(consumers.size) { null }

        launch {
            while (consume) {
                consumers.forEachIndexed { index, consumer ->
                    if (!startedConsumerIndexes[index]) {
                        val job = launch {
                            while (true) {
                                ensureActive()
                                accept(consumer)
                                delay(consumer.config.sleepTime)
                            }
                        }

                        startedConsumerIndexes[index] = true
                        jobIndexes[index] = job
                    }
                }

                if (startedConsumerIndexes.none { it }) {
                    consume = false
                    break
                }

                delay(consumersActivityCheckTimeout)
            }
        }

        jobs = jobIndexes
    }

    suspend fun stopPolling() {
        consume = false
        jobs.forEach { it?.cancelAndJoin() }
    }

    private suspend fun <T> accept(consumer: SqsConsumer<T>) =
        channelFlow {
                repeat(consumer.config.concurrency) {
                    launch {
                        sqsClient
                            .receiveMessage(consumer.receiveRequest())
                            .messages
                            .orEmpty()
                            .forEach { message ->
                                launch {
                                    if (consumer.onSuccess(message)) {
                                        send(message)
                                    } else {
                                        consumer.onError(
                                            ConsumptionError.UnsuccessfulConsumption(message)
                                        )
                                    }
                                }
                            }
                    }
                }
            }
            .catch { ex ->
                when (ex) {
                    is ClientException -> consumer.handleConsumptionException(ex)
                    is ServiceException -> consumer.handleConsumptionException(ex)
                    else -> throw ex
                }
            }
            .collect { message -> acknowledge(consumer.queueUri, message) }

    private suspend fun acknowledge(url: URL, message: Message) {
        try {
            sqsClient.deleteMessage(
                DeleteMessageRequest {
                    queueUrl = url.toString()
                    receiptHandle = message.receiptHandle
                }
            )
        } catch (e: ClientException) {
            logger.error(e) { "Failed to delete message batch" }
        }
    }
}

private suspend fun <T> SqsConsumer<T>.handleConsumptionException(exception: Throwable) {
    val error =
        when (exception) {
            is AwsServiceException -> ConsumptionError.AwsServiceError(exception = exception)
            is ClientException -> ConsumptionError.AwsClientError(exception = exception)
            else -> ConsumptionError.UnrecognizedError(exception)
        }

    onError(error)
}

private fun <T> SqsConsumer<T>.receiveRequest() = ReceiveMessageRequest {
    config.receiveRequestConfigurationBuilder(this)
    queueUrl = queueUri.toString()
}
