package com.katanox.tabour.configuration

import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import software.amazon.awssdk.awscore.exception.AwsErrorDetails

internal interface Config

sealed interface ConsumptionError {
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError
    data class UnrecognizedError(val exception: Exception): ConsumptionError
}

enum class IntegrationType {
    SQS
}

internal interface Consumer<T, K : ConsumptionError> {
    val type: IntegrationType
    var onSuccess: (T) -> Unit
    var onError: (K) -> Unit
}

fun sqsConsumerConfiguration(init: SqsConsumerConfiguration.() -> Unit): SqsConsumerConfiguration =
    config(SqsConsumerConfiguration(), init)

fun sqsConsumer(init: SqsConsumer.() -> Unit): SqsConsumer = config(SqsConsumer(), init)

private fun <T : Config> config(conf: T, init: T.() -> Unit): T = conf.apply { init() }
