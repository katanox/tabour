package com.katanox.tabour.consumption

import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.services.sqs.model.Message

sealed interface ConsumptionError {
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError

    data class UnsuccessfulConsumption(val message: Message): ConsumptionError
    data class UnrecognizedError(val exception: Exception) : ConsumptionError
}