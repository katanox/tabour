package com.katanox.tabour.integration.sqs.core.consumer

import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired

private val logger = KotlinLogging.logger {}

class SqsEventFetcher(
    private val queueUrl: String
) {

    @Autowired
    private lateinit var sqsConfiguration: SqsConfiguration

    @Autowired
    private lateinit var properties: EventPollerProperties

    fun fetchMessages(): List<Message> {
        val request = ReceiveMessageRequest()
            .withMaxNumberOfMessages(properties.batchSize)
            .withQueueUrl(queueUrl)
            .withWaitTimeSeconds(properties.waitTime.seconds.toInt())
            .withVisibilityTimeout(properties.visibilityTimeout.seconds.toInt())
        val result = sqsConfiguration.amazonSQSAsync().receiveMessage(request)
        if (result.sdkHttpMetadata == null) {
            logger.error(
                "cannot determine success from response for SQS queue {}: {}",
                queueUrl,
                result.sdkResponseMetadata
            )
            return emptyList()
        }
        if (result.sdkHttpMetadata.httpStatusCode != 200) {
            logger.error(
                "got error response from SQS queue {}: {}",
                queueUrl,
                result.sdkHttpMetadata
            )
            return emptyList()
        }
        return result.messages
    }
}