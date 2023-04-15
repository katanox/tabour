package com.katanox.tabour.config

import java.time.Duration
import java.time.temporal.ChronoUnit
import software.amazon.awssdk.services.sqs.model.Message

enum class IntegrationType {
    SQS
}

sealed interface Consumer<T> {
    val type: IntegrationType
    var successFn: (T) -> Unit
    var errorFn: (Exception) -> Unit
}

class SqsConfiguration : Consumer<Message> {
    override val type = IntegrationType.SQS

    var queueUrl: String = ""
        set(value) {
            if (value.isEmpty()) {
                throw IllegalArgumentException("Url must not be empty")
            }

            field = value
        }
    var maxMessages: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }
    val waitTime: Duration = Duration.of(30, ChronoUnit.SECONDS)
    var workers: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }
    var sleepTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)
    override var successFn: (Message) -> Unit = {}
    override var errorFn: (Exception) -> Unit = {}
}
