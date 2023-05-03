package com.katanox.tabour.consumption

import software.amazon.awssdk.awscore.exception.AwsErrorDetails

sealed interface ConsumptionError {
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError
    data class UnrecognizedError(val exception: Exception) : ConsumptionError
}