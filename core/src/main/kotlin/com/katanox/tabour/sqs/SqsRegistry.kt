package com.katanox.tabour.sqs

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

class SqsRegistry<T>
private constructor(
    private val configuration: Configuration<T>,
) : Registry<T> {
    override val key: T
        get() = configuration.key

    private val consumers: MutableList<SqsConsumer> = mutableListOf()
    private val producers: MutableSet<SqsProducer<*>> = mutableSetOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder()
            .credentialsProvider(configuration.credentialsProvider)
            .region(configuration.region)
            .build()

    private val sqsProducerExecutor = SqsProducerExecutor(sqs)
    private val sqsPoller = SqsPoller(sqs, sqsProducerExecutor)

    /**
     * Registers a consumer for a specific SQS queue After registering the consumers, use
     * [startConsumption] to start the consumption process
     */
    fun addConsumer(consumer: SqsConsumer): SqsRegistry<T> = this.apply { consumers.add(consumer) }

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

    fun <T> addProducer(producer: SqsProducer<T>) = this.apply { producers.add(producer) }

    suspend fun <T> produce(producerKey: T, produceFn: () -> Pair<String?, String?>) {
        val producer = producers.find { it.key == producerKey }

        if (producer != null) {
            sqsProducerExecutor.produce(producer, produceFn)
        }
    }

    companion object {
        internal fun <T> create(
            key: T,
            credentialsProvider: AwsCredentialsProvider,
            region: Region
        ): SqsRegistry<T> = SqsRegistry(Configuration(key, credentialsProvider, region))
    }

    private class Configuration<T>(
        val key: T,
        val credentialsProvider: AwsCredentialsProvider,
        val region: Region
    )
}
