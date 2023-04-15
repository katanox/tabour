package com.katanox.tabour.config

import java.time.Duration
import java.time.temporal.ChronoUnit
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.services.sqs.model.Message

enum class IntegrationType {
    SQS
}

sealed interface ConsumptionError {
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError
}

sealed interface Consumer<T, K : ConsumptionError> {
    val type: IntegrationType
    var successFn: (T) -> Unit
    var errorFn: (K) -> Unit
}

class SqsConfiguration : Consumer<Message, ConsumptionError.AwsError> {
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
    override var errorFn: (ConsumptionError.AwsError) -> Unit = {}
}
