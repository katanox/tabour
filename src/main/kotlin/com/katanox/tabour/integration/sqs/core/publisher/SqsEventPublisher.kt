package com.katanox.tabour.integration.sqs.core.publisher

import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import mu.KotlinLogging
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SqsEventPublisher : IEventPublisherBase {

    @Autowired private lateinit var sqsConfiguration: SqsConfiguration

    @Autowired private lateinit var tabourConfigs: TabourAutoConfigs

    override fun getType(): BusType {
        return BusType.SQS
    }

    override fun publish(message: String, busUrl: String, messageGroupId: String?) {
        publish(message, busUrl, SendMessageRequest(), messageGroupId)
    }

     override fun delete(receiptHandle: String, busUrl: String): Boolean {
        return doDelete(receiptHandle, busUrl)
    }

    /**
     * Publishes a message with a pre-configured [SendMessageRequest] which gives you all the options
     * you may need from the underlying SQS client. Note that the `queueUrl` and `messageBody` must
     * not be set because they will be set by the this publisher.
     */
    private fun publish(
        message: String,
        busUrl: String,
        preConfiguredRequest: SendMessageRequest,
        messageGroupId: String?
    ) {
        require(preConfiguredRequest.queueUrl == null) {
            "attribute queueUrl of pre-configured request must not be set!"
        }
        require(preConfiguredRequest.messageBody == null) {
            "message body of pre-configured request must not be set!"
        }
        val retry = tabourConfigs.retryRegistry().retry("publish")
        retry.eventPublisher.onError { logger.warn("error publishing message to queue {}", busUrl) }
        retry.executeRunnable { doPublish(message, busUrl, preConfiguredRequest, messageGroupId) }
    }

    private fun doPublish(
        message: String,
        busUrl: String,
        preConfiguredRequest: SendMessageRequest,
        messageGroupId: String?
    ) {
        logger.debug("sending message {} to SQS queue {}", message, busUrl)
        val request = preConfiguredRequest.withQueueUrl(busUrl).withMessageBody(message)
        messageGroupId?.let { request.withMessageGroupId(it) }
        val result = sqsConfiguration.amazonSQSAsync().sendMessage(request)
        if (result.sdkHttpMetadata.httpStatusCode != HttpStatus.SC_OK) {
            throw RuntimeException(
                String.format("got error response from SQS queue %s: %s", busUrl, result.sdkHttpMetadata)
            )
        }
        logger.info("Sent message ID {}", result.messageId)
    }

    private fun doDelete(receiptHandle: String, busUrl: String): Boolean {
        val request = DeleteMessageRequest()
            .withQueueUrl(busUrl)
            .withReceiptHandle(receiptHandle)

        return runCatching { sqsConfiguration.amazonSQSAsync().deleteMessage(request) }.isSuccess
    }
}
