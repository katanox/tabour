package com.katanox.tabour.sqs

import com.katanox.tabour.sqs.config.SqsQueueConfiguration
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.Registry
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI

class SqsRegistry(credentialsProvider: AwsCredentialsProvider, region: Region) : Registry {
    private val consumers: MutableList<SqsQueueConfiguration> = mutableListOf()
    private val sqs: SqsAsyncClient =
        SqsAsyncClient.builder().credentialsProvider(credentialsProvider)
            .endpointOverride(URI("http://localhost:4566"))
            .region(region).build()

    /**
     * Registers a consumer for a specific SQS queue After registering the consumers, use
     * [startConsumption] to start the consumption process
     */
    fun addConsumer(consumer: SqsQueueConfiguration): SqsRegistry =
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
