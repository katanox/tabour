package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.exception.ExceptionHandler
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
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
    private val eventFetcher: SqsEventFetcher,
    private val pollingProperties: EventPollerProperties,
    private val sqsConfiguration: SqsConfiguration,
    private val exceptionHandler: ExceptionHandler,
) {
    @OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun start() {
        logger.info("starting SqsMessagePoller ${LocalDateTime.now()}")
        GlobalScope.launch {
            launch {
                val channel = Channel<Message>()
                repeat(100) { pollMessages(channel) }
                repeat(100) { consumeChannel(channel) }
            }
        }
    }

    fun stop() {
        logger.info("stopping SqsMessagePoller")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun pollMessages(channel: SendChannel<Message>) {
        try {
            while (true) {
                eventFetcher.fetchMessages(queueUrl).forEach { channel.send(it) }
            }
        } catch (e: Exception) {
            logger.error("error fetching messages from queue $eventHandler.sqsQueueUrl : ${e.message}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun consumeChannel(channel: ReceiveChannel<Message>) {
        for (msg in channel) {
            handleMessage(msg)
        }
    }

    private fun handleMessage(sqsMessage: Message) {
        runBlocking {
            logger.info("Received message ID {}", sqsMessage.messageId())
            val message = sqsMessage.body()
            try {
                eventHandler.onBeforeHandle(message)
                eventHandler.handle(message)
                acknowledgeMessage(sqsMessage)
                logger.debug(
                    "message {} processed successfully - message has been deleted from SQS",
                    sqsMessage.messageId()
                )
            } catch (e: Exception) {
                when (exceptionHandler.handleException(sqsMessage, e)) {
                    ExceptionHandler.ExceptionHandlerDecision.RETRY -> {}
                    ExceptionHandler.ExceptionHandlerDecision.DELETE -> acknowledgeMessage(sqsMessage)
                }
            } finally {
                eventHandler.onAfterHandle(message)
            }
        }
    }

    private fun acknowledgeMessage(message: Message) {
        sqsConfiguration.amazonSQSAsync().deleteMessage(
            DeleteMessageRequest.builder()
                .queueUrl(eventHandler.sqsQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build()
        )
    }
}
