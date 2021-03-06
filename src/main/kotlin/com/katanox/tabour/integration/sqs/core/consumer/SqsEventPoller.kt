package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.extentions.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
) : CoroutineScope {

    private val supervisorJob = SupervisorJob()

    override val coroutineContext
        get() = Dispatchers.IO + supervisorJob

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun start() = launch {
        logger.debug("starting SqsMessagePoller ${LocalDateTime.now()}")
        withContext(Dispatchers.IO) {
            repeat(pollerConfigs.numOfPollers) {
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

    fun stop() {
        logger.info("stopping SqsMessagePoller")
        supervisorJob.cancelChildren()
        supervisorJob.cancel()
    }

    private fun handleMessage() {
        val messages = retrieveMessages()
        val messagesToBeDeleted = mutableListOf<DeleteMessageBatchRequestEntry>()
        messages.forEach { message ->
            runCatching {
                processMessage(message)
            }.onFailure { throwable ->
                eventHandler.failureAction(throwable, message)
            }.onSuccess {
                messagesToBeDeleted.add(
                    DeleteMessageBatchRequestEntry.builder().id(it.messageId())
                        .receiptHandle(it.receiptHandle())
                        .build()
                )
            }
        }
        logger.debug { "number of messages to be deleted ${messagesToBeDeleted.size}" }
        acknowledgeMessage(messagesToBeDeleted)
    }

    private fun retrieveMessages(): List<Message> {
        val request = ReceiveMessageRequest.builder().maxNumberOfMessages(pollerConfigs.batchSize).queueUrl(queueUrl)
            .waitTimeSeconds(pollerConfigs.waitTime.toSeconds().toInt())
            .visibilityTimeout(pollerConfigs.visibilityTimeout.toSeconds().toInt()).build()
        return client.receiveMessage(request).messages()
    }

    private fun processMessage(sqsMessage: Message): Message {
        val message = sqsMessage.body()
        eventHandler.onBeforeHandle(message)
        eventHandler.handle(message)
        eventHandler.onAfterHandle(message)
        return sqsMessage
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
        logger.debug { "${messagesToDelete.size} messages has been deleted from SQS" }
    }
}
