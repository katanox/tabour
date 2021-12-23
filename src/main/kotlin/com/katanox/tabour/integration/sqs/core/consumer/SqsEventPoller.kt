package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.extentions.retry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.LocalDateTime
import kotlin.streams.toList
import kotlin.time.ExperimentalTime

/**
 * Polls messages from an SQS queue in potentially multiple threads at regular intervals.
 *
 * @param <T> the type of message.
 */
private val logger = KotlinLogging.logger {}

class SqsEventPoller(
    private val queueUrl: String,
    private val eventHandler: SqsEventHandler,
    private val client: SqsClient,
    private val pollerConfigs: EventPollerProperties,
    private val tabourConfigs: TabourAutoConfigs,
) {

    private lateinit var job: Job

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun start() {
        logger.debug("starting SqsMessagePoller ${LocalDateTime.now()}")
        job = GlobalScope.launch {
            withContext(Dispatchers.IO) {
                repeat(pollerConfigs.pollingCoroutines) {
                    async {
                        logger.debug("starting coroutine-$it ${LocalDateTime.now()}")
                        while (true) {
                            handleMessage()
                            pollerConfigs.pollDelay?.toMillis()?.let {
                                delay(it)
                            }
                        }
                    }.start()
                }
            }
        }
    }

    fun stop() {
        logger.info("stopping SqsMessagePoller")
        job.cancelChildren()
        job.cancel()
    }

    private fun handleMessage() {
        val messages = retrieveMessages()
        val messagesToDelete = messages
            .parallelStream()
            .filter {
                processMessage(it)
            }.map {
                DeleteMessageBatchRequestEntry.builder()
                    .id(it.messageId())
                    .receiptHandle(it.receiptHandle())
                    .build()
            }.toList()
        logger.debug { "number of messages to be deleted ${messagesToDelete.size}" }
        acknowledgeMessage(messagesToDelete)
    }

    private fun retrieveMessages(): List<Message> {
        val request =
            ReceiveMessageRequest.builder()
                .maxNumberOfMessages(pollerConfigs.batchSize)
                .queueUrl(queueUrl)
                .waitTimeSeconds(pollerConfigs.waitTime.toSeconds().toInt())
                .visibilityTimeout(pollerConfigs.visibilityTimeout.toSeconds().toInt())
                .build()
        return client
            .receiveMessage(request)
            .messages()
    }

    private fun processMessage(sqsMessage: Message): Boolean {
        return try {
            val message = sqsMessage.body()
            eventHandler.onBeforeHandle(message)
            eventHandler.handle(message)
            logger.debug { "message ${sqsMessage.messageId()} processed successfully - message has been deleted from SQS" }
            eventHandler.onAfterHandle(message)
            true
        } catch (exception: Exception) {
            logger.warn { "error happened while processing the message " }
            false
        }
    }

    private fun acknowledgeMessage(messagesToDelete: List<DeleteMessageBatchRequestEntry>) {
        runBlocking {
            retry(times = tabourConfigs.tabourProperties.maxRetryCount) {
                if (messagesToDelete.isNotEmpty()) {
                    client.deleteMessageBatch(
                        DeleteMessageBatchRequest.builder()
                            .queueUrl(eventHandler.sqsQueueUrl)
                            .entries(messagesToDelete)
                            .build()
                    )
                }
            }
        }
    }
}
