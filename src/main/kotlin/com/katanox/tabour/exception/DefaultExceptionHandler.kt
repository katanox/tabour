package com.katanox.tabour.exception

import mu.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.model.Message

private val logger = KotlinLogging.logger {}

@Component
class DefaultExceptionHandler : ExceptionHandler {

    override fun handleException(
        message: Message,
        e: Exception,
    ): ExceptionHandler.ExceptionHandlerDecision {
        logger.warn(
            "error while processing message ${message.messageId()}" +
                " - message has not been deleted from SQS and will be retried: $e",
        )
        return ExceptionHandler.ExceptionHandlerDecision.RETRY
    }
}
