package com.katanox.tabour.configuration.sqs

import aws.sdk.kotlin.services.sqs.model.Message
import com.katanox.tabour.configuration.core.config
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.SqsRegistry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.config.SqsConsumerConfiguration
import com.katanox.tabour.sqs.production.ProductionError
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerConfiguration
import java.net.URL

/**
 * Creates a new [SqsRegistry.Configuration] which can be used as configuration for a [SqsRegistry]
 *
 * [init] is used to modify the default values of the configuration
 */
fun <T> sqsRegistryConfiguration(
    key: T,
    region: String,
    init: SqsRegistry.Configuration<T>.() -> Unit,
): SqsRegistry.Configuration<T> = config(SqsRegistry.Configuration(key, region), init)

/**
 * Creates a new [SqsRegistry.Configuration] which can be used as configuration for a [SqsRegistry]
 * using the default configuration values
 */
fun <T> sqsRegistryConfiguration(key: T, region: String): SqsRegistry.Configuration<T> =
    SqsRegistry.Configuration(key, region)

/** Creates a new [SqsRegistry] */
fun <T> sqsRegistry(config: SqsRegistry.Configuration<T>): SqsRegistry<T> = SqsRegistry(config)

/**
 * Creates a new [SqsConsumer] which can be registered to [SqsRegistry]. The [url] is the url of the
 * queue and uses [init] to configure the consumers properties
 */
fun <T> sqsConsumer(
    url: URL,
    key: T,
    onSuccess: suspend (Message) -> Boolean,
    onError: suspend (ConsumptionError) -> Unit,
    init: SqsConsumer<T>.() -> Unit,
): SqsConsumer<T> = config(SqsConsumer(url, key, onSuccess, onError), init)

/**
 * Creates a new [SqsConsumer] which can be registered to [SqsRegistry]. The [url] is the url of the
 * queue
 */
fun <T> sqsConsumer(
    url: URL,
    key: T,
    onSuccess: suspend (Message) -> Boolean,
    onError: suspend (ConsumptionError) -> Unit,
): SqsConsumer<T> = SqsConsumer(url, key, onSuccess, onError)

/**
 * Creates a new [SqsProducer] which can be registered to [SqsRegistry]. The [url] is the url of the
 * queue
 */
fun <T> sqsProducer(
    url: URL,
    key: T,
    onError: suspend (ProductionError) -> Unit,
    init: SqsProducer<T>.() -> Unit,
): SqsProducer<T> = config(SqsProducer(key, url, onError), init)

fun <T> sqsProducer(url: URL, key: T, onError: (ProductionError) -> Unit): SqsProducer<T> =
    SqsProducer(key, url, onError)

/** Creates a new [SqsConsumerConfiguration] which can be used to configure a [SqsConsumer] */
fun sqsConsumerConfiguration(init: SqsConsumerConfiguration.() -> Unit): SqsConsumerConfiguration =
    config(SqsConsumerConfiguration(), init)

/** Creates a new [SqsProducerConfiguration] which can be used to configure a [SqsProducer] */
fun sqsProducerConfiguration(init: SqsProducerConfiguration.() -> Unit): SqsProducerConfiguration =
    config(SqsProducerConfiguration(), init)
