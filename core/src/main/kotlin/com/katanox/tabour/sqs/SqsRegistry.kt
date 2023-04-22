package com.katanox.tabour.sqs

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.sqs.production.SqsProducer
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import java.net.URI
import kotlinx.coroutines.coroutineScope
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

class SqsRegistry
private constructor(
    credentialsProvider: AwsCredentialsProvider,
    region: Region,
    override val key: String
) : Registry {
    private val consumers: MutableList<SqsConsumer> = mutableListOf()
    private val producers: MutableSet<SqsProducer> = mutableSetOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .endpointOverride(URI("http://localhost:4566"))
            .region(region)
            .build()
    private val sqsProducerExecutor = SqsProducerExecutor(sqs)
    private val sqsPoller = SqsPoller(sqs)

    /**
     * Registers a consumer for a specific SQS queue After registering the consumers, use
     * [startConsumption] to start the consumption process
     */
    fun addConsumer(consumer: SqsConsumer): SqsRegistry = this.apply { consumers.add(consumer) }

    /**
     * Starts the consuming process using the Consumers that have been registered up until the time
     * when start was used
     */
    override suspend fun startConsumption() {
        sqsPoller.poll(consumers)
    }

    suspend fun startConsumption(f: () -> Boolean) {
        sqsPoller.poll(consumers, f)
    }

    override suspend fun stopConsumption() {
        sqsPoller.stopPolling()
    }

    fun addProducer(producer: SqsProducer) = this.apply { producers.add(producer) }

    suspend fun produce(producerKey: String) = coroutineScope {
        producers.find { it.key == producerKey }?.also { sqsProducerExecutor.produce(it) }
    }

    companion object {
        fun create(
            credentialsProvider: AwsCredentialsProvider,
            region: Region,
            key: String
        ): SqsRegistry = SqsRegistry(credentialsProvider, region, key)
    }
}
