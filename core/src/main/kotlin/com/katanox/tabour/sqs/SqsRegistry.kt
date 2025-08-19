package com.katanox.tabour.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.error.ProducerNotFound
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.sqs.production.SqsDataProductionConfiguration
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URI
import software.amazon.awssdk.regions.Region

/**
 * An implementation of [Registry] which works with SQS
 *
 * [T] is the key of the registry which is used to identify the registry
 */
class SqsRegistry<T> internal constructor(private val configuration: Configuration<T>) :
    Registry<T> {
    override val key: T
        get() = configuration.key

    private val consumers: MutableList<SqsConsumer<*>> = mutableListOf()
    private val producers: MutableSet<SqsProducer<*>> = mutableSetOf()

    private val sqsProducerExecutor: SqsProducerExecutor = SqsProducerExecutor()
    private var sqsPoller: SqsPoller? = null

    /** Adds a consumer to the registry */
    fun addConsumer(consumer: SqsConsumer<*>): SqsRegistry<T> =
        this.apply { consumers.add(consumer) }

    /** Adds a collection of consumers to the registry */
    fun addConsumers(consumers: List<SqsConsumer<*>>): SqsRegistry<T> =
        this.apply { consumers.fold(this) { registry, consumer -> registry.addConsumer(consumer) } }

    /** Adds a producer to the registry */
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
        val sqs = buildSqsClient()
        SqsPoller(sqs).also { sqsPoller = it }.poll(consumers)
    }

    /** Stops the consumption of messages by waiting the consumers to finish their work */
    override suspend fun stopConsumption() {
        sqsPoller?.stopPolling()
    }

    /** Produces an SQS message using the producer that was registered with [producerKey] */
    suspend fun <T> produce(
        producerKey: T,
        productionConfiguration: SqsDataProductionConfiguration,
    ) {
        val producer = producers.find { it.key == producerKey }

        if (producer != null) {
            SqsClient.fromEnvironment {
                    region = configuration.region.id()
                    endpointUrl = configuration.endpointOverride?.toString()?.let { Url.parse(it) }
                    credentialsProvider = configuration.credentialsProvider
                }
                .use { client ->
                    sqsProducerExecutor.produce(client, producer, productionConfiguration)
                }
        } else {
            productionConfiguration.resourceNotFound(ProducerNotFound(producerKey))
        }
    }

    class Configuration<T>(
        /** Key of the registry */
        val key: T,
        /** The region of the credentials */
        val region: Region,
    ) : Config {
        /**
         * Can be used to change the endpoint of the SQS queue. Usually this is for testing purposes
         * with Localstack
         */
        var endpointOverride: URI? = null

        /**
         * Explicit credentials provider. If not set, the
         * [default chain](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html)
         * is used
         */
        var credentialsProvider: CredentialsProvider? = null
    }

    private suspend fun buildSqsClient(): SqsClient =
        SqsClient.fromEnvironment {
            region = configuration.region.id()
            endpointUrl = configuration.endpointOverride?.toString()?.let(Url::parse)
            credentialsProvider = configuration.credentialsProvider
        }
}
