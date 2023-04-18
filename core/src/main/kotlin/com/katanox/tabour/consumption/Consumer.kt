package com.katanox.tabour.consumption

import software.amazon.awssdk.awscore.exception.AwsErrorDetails

internal interface Config

sealed interface ConsumptionError {
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError
    data class UnrecognizedError(val exception: Exception) : ConsumptionError
}

internal interface Consumer<T, K : ConsumptionError> {
    var onSuccess: (T) -> Unit
    var onError: (K) -> Unit
}
