package com.katanox.tabour.sqs

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.sqs.production.SqsDataForProduction
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URI
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

/**
 * A type of [Registry] which works with SQS
 *
 * [T] is the key of the registry which is used to identify the registry
 *
 * Supports consumption and production of SQS messages
 */
class SqsRegistry<T>
internal constructor(
    private val configuration: Configuration<T>,
) : Registry<T> {
    override val key: T
        get() = configuration.key

    private val consumers: MutableList<SqsConsumer> = mutableListOf()
    private val producers: MutableSet<SqsProducer<*>> = mutableSetOf()
    private val sqs: SqsClient =
        SqsClient.builder()
            .credentialsProvider(configuration.credentialsProvider)
            .region(configuration.region)
            .apply {
                if (configuration.endpointOverride != null) {
                    this.endpointOverride(configuration.endpointOverride)
                }
            }
            .build()

    private val sqsProducerExecutor = SqsProducerExecutor(sqs)
    private val sqsPoller = SqsPoller(sqs, sqsProducerExecutor)

    /**
     * Adds a consumer to the registry which can be later started with other consumers, using
     * [startConsumption]
     */
    fun addConsumer(consumer: SqsConsumer): SqsRegistry<T> = this.apply { consumers.add(consumer) }

    /** Adds a collection of consumers to the registry */
    fun addConsumers(consumers: List<SqsConsumer>): SqsRegistry<T> =
        this.apply { consumers.fold(this) { registry, consumer -> registry.addConsumer(consumer) } }

    fun <K> addProducer(producer: SqsProducer<K>): SqsRegistry<T> =
        this.apply { producers.add(producer) }

    /** Adds a collection of producers to the registry */
    fun <K> addProducers(producers: List<SqsProducer<K>>): SqsRegistry<T> =
        this.apply { producers.fold(this) { registry, producer -> registry.addProducer(producer) } }

    /**
     * Starts the consuming process using the Consumers that have been registered up until the time
     * when start was used
     */
    override suspend fun startConsumption() {
        sqsPoller.poll(consumers)
    }

    override suspend fun stopConsumption() {
        sqsPoller.stopPolling()
    }

    suspend fun <T> produce(producerKey: T, produceFn: () -> SqsDataForProduction) {
        val producer = producers.find { it.key == producerKey }

        if (producer != null) {
            sqsProducerExecutor.produce(producer, produceFn)
        }
    }

    class Configuration<T>(
        val key: T,
        val credentialsProvider: AwsCredentialsProvider,
        val region: Region,
    ) : Config {
        var endpointOverride: URI? = null
    }
}
