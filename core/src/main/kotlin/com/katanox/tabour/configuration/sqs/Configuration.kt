package com.katanox.tabour.configuration.sqs

import com.katanox.tabour.Tabour
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import com.katanox.tabour.sqs.config.SqsPipeline
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerConfiguration

fun sqsConsumerConfiguration(init: SqsConsumerConfiguration.() -> Unit): SqsConsumerConfiguration =
    config(SqsConsumerConfiguration(), init)

fun sqsConsumer(init: SqsConsumer.() -> Unit): SqsConsumer = config(SqsConsumer(), init)

fun <T> sqsProducer(keyInit: () -> T, init: SqsProducer<T>.() -> Unit): SqsProducer<T> =
    config(SqsProducer(keyInit), init)

fun sqsProducerConfiguration(init: SqsProducerConfiguration.() -> Unit): SqsProducerConfiguration =
    config(SqsProducerConfiguration(), init)

fun tabour(init: Tabour.() -> Unit): Tabour = config(Tabour(), init)

fun sqsPipeline(init: SqsPipeline.() -> Unit): SqsPipeline = config(SqsPipeline(), init)

private fun <T : Config> config(conf: T, init: T.() -> Unit): T = conf.apply { init() }
