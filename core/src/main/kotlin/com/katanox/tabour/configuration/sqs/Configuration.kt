package com.katanox.tabour.configuration.sqs

import com.katanox.tabour.Tabour
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.SqsRegistry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import com.katanox.tabour.sqs.config.SqsPipeline
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerConfiguration
import java.net.URL
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region

fun sqsConsumer(
    configuration: SqsConsumerConfiguration,
    init: SqsConsumer.() -> Unit
): SqsConsumer = config(SqsConsumer(configuration), init)

fun <T> sqsProducer(key: T, init: SqsProducer<T>.() -> Unit): SqsProducer<T> =
    config(SqsProducer(key), init)

fun sqsConsumerConfiguration(
    url: URL,
    init: SqsConsumerConfiguration.() -> Unit
): SqsConsumerConfiguration = config(SqsConsumerConfiguration(url), init)

fun sqsConsumerConfiguration(
    url: URL,
): SqsConsumerConfiguration = SqsConsumerConfiguration(url)

fun sqsProducerConfiguration(init: SqsProducerConfiguration.() -> Unit): SqsProducerConfiguration =
    config(SqsProducerConfiguration(), init)

fun tabour(init: Tabour.Configuration.() -> Unit): Tabour =
    Tabour(config(Tabour.Configuration(), init))

fun sqsPipeline(init: SqsPipeline.() -> Unit): SqsPipeline = config(SqsPipeline(), init)

fun <T> sqsRegistry(
    key: T,
    credentialsProvider: AwsCredentialsProvider,
    region: Region
): SqsRegistry<T> = SqsRegistry.create(key, credentialsProvider, region)

private fun <T : Config> config(conf: T, init: T.() -> Unit): T = conf.apply { init() }
