package com.katanox.tabour.configuration

import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerConfiguration
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

fun sqsConsumerConfiguration(init: SqsConsumerConfiguration.() -> Unit): SqsConsumerConfiguration =
    config(SqsConsumerConfiguration(), init)

fun sqsConsumer(init: SqsConsumer.() -> Unit): SqsConsumer = config(SqsConsumer(), init)

fun sqsProducer(init: SqsProducer.() -> Unit): SqsProducer = config(SqsProducer(), init)

fun sqsProducerConfiguration(init: SqsProducerConfiguration.() -> Unit): SqsProducerConfiguration =
    config(SqsProducerConfiguration(), init)

private fun <T : Config> config(conf: T, init: T.() -> Unit): T = conf.apply { init() }
