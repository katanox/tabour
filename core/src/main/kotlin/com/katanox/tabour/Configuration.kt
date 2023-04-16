package com.katanox.tabour

import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import com.katanox.tabour.sqs.config.SqsQueueConfiguration
import software.amazon.awssdk.awscore.exception.AwsErrorDetails

internal interface Config

sealed interface ConsumptionError {
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError
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

fun sqsQueueConfiguration(init: SqsQueueConfiguration.() -> Unit): SqsQueueConfiguration =
    config(SqsQueueConfiguration(), init)

private inline fun <T : Config> config(conf: T, init: T.() -> Unit): T {
    conf.init()
    return conf
}
