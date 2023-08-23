package com.katanox.tabour.configuration.sqs

import com.katanox.tabour.configuration.core.config
import com.katanox.tabour.sqs.SqsRegistry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import com.katanox.tabour.sqs.config.SqsPipeline
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerConfiguration
import java.net.URL
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region

fun <T> sqsRegistryConfiguration(
    key: T,
    credentialsProvider: AwsCredentialsProvider,
    region: Region,
    init: SqsRegistry.Configuration<T>.() -> Unit
): SqsRegistry.Configuration<T> =
    config(SqsRegistry.Configuration(key, credentialsProvider, region), init)

fun <T> sqsRegistryConfiguration(
    key: T,
    credentialsProvider: AwsCredentialsProvider,
    region: Region
): SqsRegistry.Configuration<T> = SqsRegistry.Configuration(key, credentialsProvider, region)

/** Creates a new [SqsRegistry] */
fun <T> sqsRegistry(config: SqsRegistry.Configuration<T>): SqsRegistry<T> = SqsRegistry(config)

/**
 * Creates a new [SqsConsumer] which can be registered to [SqsRegistry]. The [url] is the url of the
 * queue
 */
fun sqsConsumer(url: URL, init: SqsConsumer.() -> Unit): SqsConsumer =
    config(SqsConsumer(url), init)

/**
 * Creates a new [SqsProducer] which can be registered to [SqsRegistry]. The [url] is the url of the
 * queue
 */
fun <T> sqsProducer(url: URL, key: T, init: SqsProducer<T>.() -> Unit): SqsProducer<T> =
    config(SqsProducer(key, url), init)

fun <T> sqsProducer(url: URL, key: T): SqsProducer<T> = SqsProducer(key, url)

/** Creates a new [SqsConsumerConfiguration] which can be used to configure a [SqsConsumer] */
fun sqsConsumerConfiguration(init: SqsConsumerConfiguration.() -> Unit): SqsConsumerConfiguration =
    config(SqsConsumerConfiguration(), init)

/** Creates a new [SqsProducerConfiguration] which can be used to configure a [SqsProducer] */
fun sqsProducerConfiguration(init: SqsProducerConfiguration.() -> Unit): SqsProducerConfiguration =
    config(SqsProducerConfiguration(), init)

/**
 * Creates a new [SqsPipeline] which can be used on a [SqsConsumer] to build a pipeline of messages
 * consumption-production
 */
fun sqsPipeline(init: SqsPipeline.() -> Unit): SqsPipeline = config(SqsPipeline(), init)
