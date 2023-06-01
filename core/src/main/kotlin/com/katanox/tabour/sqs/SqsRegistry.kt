package com.katanox.tabour.sqs

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import kotlinx.coroutines.coroutineScope
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

class SqsRegistry<T>
private constructor(
    credentialsProvider: AwsCredentialsProvider,
    region: Region,
    override val key: T
) : Registry<T> {
    private val consumers: MutableList<SqsConsumer> = mutableListOf()
    private val producers: MutableSet<SqsProducer<*>> = mutableSetOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder().credentialsProvider(credentialsProvider).region(region).build()
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

    suspend fun <T> produce(producerKey: T, produceFn: () -> String?) = coroutineScope {
        val producer = producers.find { it.key == producerKey }

        if (producer != null) {
            sqsProducerExecutor.produce(producer, produceFn)
        }
    }

    companion object {
        fun <K> create(
            credentialsProvider: AwsCredentialsProvider,
            region: Region,
            key: K
        ): SqsRegistry<K> = SqsRegistry(credentialsProvider, region, key)
    }
}
