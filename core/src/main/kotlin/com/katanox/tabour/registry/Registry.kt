package com.katanox.tabour.registry

import com.katanox.tabour.config.SqsConfiguration
import com.katanox.tabour.consumer.SqsConsumer
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region

sealed interface Registry {
    suspend fun start()
}

class SqsRegistry(
    private val credentialsProvider: AwsCredentialsProvider,
    private val region: Region
) : Registry {
    private val consumers: MutableList<SqsConfiguration> = mutableListOf()

    /**
     * Registers a consumer for a specific SQS queue
     * After registering the consumers, use [start] to start the consumption process
     */
    fun addConsumer(consumer: SqsConfiguration): SqsRegistry {
        consumers.add(consumer)
        return this
    }

    /**
     * Starts the consuming process using the Consumers that have been registered
     * up until the time where start was used
     */
    override suspend fun start() {
        val sqsConsumer = SqsConsumer(credentialsProvider, region)
        sqsConsumer.start(consumers)
    }
}
