package com.katanox.tabour.exception

import com.amazonaws.services.sqs.model.Message
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DefaultExceptionHandler : ExceptionHandler {

    override fun handleException(message: Message, e: Exception): ExceptionHandler.ExceptionHandlerDecision {
        logger.error(
            "error while processing message ${message.messageId} - message has not been deleted from SQS and will be retried:  $e",
        )
        return ExceptionHandler.ExceptionHandlerDecision.RETRY
    }
}
