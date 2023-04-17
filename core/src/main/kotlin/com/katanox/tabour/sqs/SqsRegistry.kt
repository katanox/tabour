package com.katanox.tabour.sqs

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.sqs.config.SqsConsumer
import com.katanox.tabour.sqs.consumption.SqsPoller
import java.net.URI
import kotlinx.coroutines.CoroutineDispatcher
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

class SqsRegistry
private constructor(
    credentialsProvider: AwsCredentialsProvider,
    region: Region,
) : Registry {
    private val consumers: MutableList<SqsConsumer> = mutableListOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .endpointOverride(URI("http://localhost:4566"))
            .region(region)
            .build()

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
        val sqsPoller = SqsPoller.create(sqs)
        sqsPoller.poll(consumers)
    }

    companion object {
        fun create(credentialsProvider: AwsCredentialsProvider, region: Region): SqsRegistry =
            SqsRegistry(credentialsProvider, region)

    }
}
