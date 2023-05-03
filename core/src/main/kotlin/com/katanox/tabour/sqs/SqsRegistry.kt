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

class SqsRegistry
private constructor(
    credentialsProvider: AwsCredentialsProvider,
    region: Region,
    override val key: String
) : Registry {
    private val consumers: MutableList<SqsConsumer> = mutableListOf()
    private val producers: MutableSet<SqsProducer> = mutableSetOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder().credentialsProvider(credentialsProvider).region(region).build()
    private val sqsProducerExecutor = SqsProducerExecutor(sqs)
    private val sqsPoller = SqsPoller(sqs, sqsProducerExecutor)

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

    override suspend fun stopConsumption() {
        sqsPoller.stopPolling()
    }

    override fun isValid(): Boolean =
        consumers.all(SqsConsumer::handlerWasSet) && producers.all(SqsProducer::urlWasSet)

    fun addProducer(producer: SqsProducer) = this.apply { producers.add(producer) }

    suspend fun produce(producerKey: String, valueFn: () -> String) = coroutineScope {
        val producer = producers.find { it.key == producerKey }

        if (producer != null) {
            sqsProducerExecutor.produce(producer, valueFn)
        }
    }

    companion object {
        fun create(
            credentialsProvider: AwsCredentialsProvider,
            region: Region,
            key: String
        ): SqsRegistry = SqsRegistry(credentialsProvider, region, key)
    }
}
