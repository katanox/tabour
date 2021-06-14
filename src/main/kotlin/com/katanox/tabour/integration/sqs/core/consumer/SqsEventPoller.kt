package com.katanox.tabour.integration.sqs.core.consumer

import com.amazonaws.services.sqs.model.Message
import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.exception.ExceptionHandler
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Polls messages from an SQS queue in potentially multiple threads at regular intervals.
 *
 * @param <T> the type of message.
 */
private val logger = KotlinLogging.logger {}

class SqsEventPoller(
    private val name: String,
    private val eventHandler: SqsEventHandler,
    private val eventFetcher: SqsEventFetcher,
    private val pollerThreadPool: ScheduledThreadPoolExecutor,
    private val handlerThreadPool: ThreadPoolExecutor,
    private val pollingProperties: EventPollerProperties,
    private val sqsConfiguration: SqsConfiguration,
    private val exceptionHandler: ExceptionHandler
) {
    fun start() {
        logger.info("starting SqsMessagePoller")
        for (i in 0 until pollerThreadPool.corePoolSize) {
            logger.info("starting SqsMessagePoller ({}) - thread {}", name, i)
            pollerThreadPool.scheduleWithFixedDelay(
                { pollMessages() },
                pollingProperties.pollDelay.seconds,
                pollingProperties.pollDelay.seconds,
                TimeUnit.SECONDS
            )
        }
    }

    fun stop() {
        logger.info("stopping SqsMessagePoller")
        pollerThreadPool.shutdownNow()
        handlerThreadPool.shutdownNow()
    }

    private fun pollMessages() {
        try {
            val messages: List<Message> = eventFetcher.fetchMessages()
            for (sqsMessage in messages) {
                handleMessage(sqsMessage)
            }
        } catch (e: Exception) {
            logger.error("error fetching messages from queue {}:", eventHandler.sqsQueueUrl, e)
        }
    }

    private fun handleMessage(sqsMessage: Message) {
        logger.info("Received message ID {}", sqsMessage.messageId)
        val message = sqsMessage.body
        handlerThreadPool.submit {
            try {
                eventHandler.onBeforeHandle(message.toByteArray())
                eventHandler.handle(message.toByteArray())
                acknowledgeMessage(sqsMessage)
                logger.debug(
                    "message {} processed successfully - message has been deleted from SQS",
                    sqsMessage.messageId
                )
            } catch (e: Exception) {
                when (exceptionHandler.handleException(sqsMessage, e)) {
                    ExceptionHandler.ExceptionHandlerDecision.RETRY -> {
                    }
                    ExceptionHandler.ExceptionHandlerDecision.DELETE -> acknowledgeMessage(sqsMessage)
                }
            } finally {
                eventHandler.onAfterHandle(message.toByteArray())
            }
        }
    }

    private fun acknowledgeMessage(message: Message) {
        sqsConfiguration.amazonSQSAsync().deleteMessage(eventHandler.sqsQueueUrl, message.receiptHandle)
    }
}