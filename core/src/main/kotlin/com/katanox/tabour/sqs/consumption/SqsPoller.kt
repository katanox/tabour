package com.katanox.tabour.sqs.consumption

import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.plug.FailurePlugRecord
import com.katanox.tabour.plug.SuccessPlugRecord
import com.katanox.tabour.retry
import com.katanox.tabour.sqs.config.SqsConsumer
import java.net.URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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

internal class SqsPoller(private val sqs: SqsClient) {
    private var consume: Boolean = false
    private var acknowledge: Boolean = true
    private val toAcknowledge = Channel<ToBeAcknowledged>()
    private var jobs: Array<Job?> = arrayOf()

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
                                accept(consumer)
                                delay(consumer.config.sleepTime.toMillis())
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

    private suspend fun <T> handleMessages(messages: List<Message>, consumer: SqsConsumer<T>) {
        messages.forEach { message ->
            try {
                if (consumer.onSuccess(message)) {
                    consumer.notifyPlugs(message)
                    toAcknowledge.send(ToBeAcknowledged(consumer.queueUri, message))
                } else {
                    val error = ConsumptionError.UnsuccessfulConsumption(message)
                    consumer.onError(error)
                    consumer.notifyPlugs(message, error)
                }
            } catch (e: Throwable) {
                consumer.onError(ConsumptionError.ThrowableDuringHanding(e))
            }
        }
    }

    private suspend fun <T> SqsConsumer<T>.notifyPlugs(
        message: Message,
        error: ConsumptionError? = null
    ) {
        if (this.plugs.isNotEmpty()) {
            this.plugs.forEach { plug ->
                if (error == null) {
                    plug.handle(SuccessPlugRecord(message.body(), this.key))
                } else {
                    plug.handle(FailurePlugRecord(message.body(), this.key, error))
                }
            }
        }
    }

    private suspend fun startAcknowledging() {
        while (acknowledge) {
            buildList {
                    repeat(10) { toAcknowledge.tryReceive().getOrNull()?.also { this.add(it) } }
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

            delay(1000)
        }
    }
}
