package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventPollerProperties
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

private val logger = KotlinLogging.logger {}

class SqsEventFetcher(
    private val properties: EventPollerProperties,
    private val client: SqsAsyncClient
) {

    suspend fun fetchMessages(queueUrl: String): List<Message> {
        var messages: List<Message> = emptyList()

        val request = ReceiveMessageRequest.builder().maxNumberOfMessages(properties.batchSize).queueUrl(queueUrl)
            .waitTimeSeconds(properties.waitTime.seconds.toInt())
            .visibilityTimeout(properties.visibilityTimeout.seconds.toInt()).build()
        val result = client.receiveMessage(request).await()

        if (result.sdkHttpResponse() == null) {
            logger.error(
                "cannot determine success from response for SQS queue {}: {}",
                queueUrl,
                result.sdkHttpResponse()
            )
        } else {
            messages = result.messages()
        }

        return messages
    }
}
