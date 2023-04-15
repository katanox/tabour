package com.katanox.tabour.config

import java.time.Duration
import java.time.temporal.ChronoUnit
import software.amazon.awssdk.services.sqs.model.Message

enum class IntegrationType {
    SQS
}

sealed interface Consumer {
    fun <T> onSuccess(successResult: T)
    fun onFail(exception: Exception)

    val type: IntegrationType
}

class SqsConfiguration : Consumer {
    override val type = IntegrationType.SQS

    var queueUrl: String = ""
        set(value) {
            if (value.isEmpty()) {
                throw IllegalArgumentException("Url must not be empty")
            }

            field = value
        }
    var maxMessages: Int = 10
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }
    val waitTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)
    var workers: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }
    var sleepTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)
    var successFn: (Message) -> Unit = {}
    var errorFn: (Exception) -> Unit = {}

    override fun <T> onSuccess(successResult: T): Unit = successFn(successResult as Message)

    override fun onFail(exception: Exception): Unit = errorFn(exception)
}
