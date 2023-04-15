package com.katanox.tabour.registry

import com.katanox.tabour.config.SqsConfiguration
import com.katanox.tabour.consumer.SqsPoller
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

sealed interface Registry {
    suspend fun startConsumption()
}

class SqsRegistry(credentialsProvider: AwsCredentialsProvider, region: Region) : Registry {
    private val consumers: MutableList<SqsConfiguration> = mutableListOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder().credentialsProvider(credentialsProvider).region(region).build()

    /**
     * Registers a consumer for a specific SQS queue After registering the consumers, use
     * [startConsumption] to start the consumption process
     */
    fun addConsumer(consumer: SqsConfiguration): SqsRegistry =
        this.apply { consumers.add(consumer) }

    /**
     * Starts the consuming process using the Consumers that have been registered up until the time
     * when start was used
     */
    override suspend fun startConsumption() {
        val sqsPoller = SqsPoller(sqs)
        sqsPoller.poll(consumers)
    }
}
