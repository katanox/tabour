package com.katanox.tabour.integration.sqs.core.consumer
// ktlint-disable no-wildcard-imports
import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.exception.ExceptionHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
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
    private val client: SqsAsyncClient,
    private val exceptionHandler: ExceptionHandler,
) : CoroutineScope {

    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + supervisorJob

    @OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun start() = launch(Dispatchers.IO) {
        logger.info("starting SqsMessagePoller ${LocalDateTime.now()}")

        val channel = Channel<List<Message>>()

        repeat(100) {
            launch {
                pollMessages(channel)
            }
        }
        repeat(10) {
            launch {
                consumeMes(channel)
            }
        }
    }

    fun stop() {
        logger.info("stopping SqsMessagePoller")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun pollMessages(channel: SendChannel<List<Message>>) {
        while (true) {
            try {
                channel.send(eventFetcher.fetchMessages(queueUrl))
            } catch (e: Exception) {
                logger.error("error fetching messages from queue $eventHandler.sqsQueueUrl : ${e.message}")
            }

            delay(10L)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun consumeMes(channel: ReceiveChannel<List<Message>>) {
        coroutineScope {
            for (pack in channel) {
                handleMessagesPack(pack)
            }
        }
    }

    private fun handleMessagesPack(sqsMessages: List<Message>) {
        val toBeAcknowledged = mutableListOf<Message>()
        sqsMessages.forEach { sqsMessage ->
//            logger.info("Received message ID {}", sqsMessage.messageId())
            val message = sqsMessage.body()
            try {
                eventHandler.onBeforeHandle(message)
                eventHandler.handle(message)
                toBeAcknowledged.add(sqsMessage)
//            logger.debug(
//                "message {} processed successfully - message has been deleted from SQS",
//                sqsMessage.messageId()
//            )
            } catch (e: Exception) {
                when (exceptionHandler.handleException(sqsMessage, e)) {
                    ExceptionHandler.ExceptionHandlerDecision.RETRY -> {}
                    ExceptionHandler.ExceptionHandlerDecision.DELETE -> toBeAcknowledged.add(sqsMessage)
                }
            } finally {
                acknowledgeMessages(toBeAcknowledged)
                eventHandler.onAfterHandle(message)
            }
        }
    }

    private fun acknowledgeMessages(messages: List<Message>) {
        val entries = messages.map {
            DeleteMessageBatchRequestEntry.builder().id(it.messageId())
                .receiptHandle(it.receiptHandle())
                .build()
        }
        client.deleteMessageBatch(
            DeleteMessageBatchRequest.builder()
                .queueUrl(eventHandler.sqsQueueUrl)
                .entries(entries)
                .build()
        )
    }
}
