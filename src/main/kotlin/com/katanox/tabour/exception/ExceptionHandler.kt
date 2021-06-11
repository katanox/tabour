package com.katanox.tabour.exception

import com.amazonaws.services.sqs.model.Message

interface ExceptionHandler {
    enum class ExceptionHandlerDecision {
        /** Delete the message from SQS. It will not be retried.  */
        DELETE,

        /**
         * Do not delete the message from SQS. In one of the next iterations, it will be polled by the
         * poller again.
         */
        RETRY
    }

    /**
     * Handles any exception that is thrown during message processing by an [SqsMessageHandler].
     */
    fun handleException(message: Message, e: Exception): ExceptionHandlerDecision

    companion object {
        @JvmStatic
        fun defaultExceptionHandler(): ExceptionHandler {
            return DefaultExceptionHandler()
        }
    }
}