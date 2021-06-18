package com.katanox.tabour.integration.sqs.core.publisher

import com.amazonaws.services.sqs.model.SendMessageRequest
import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable

private val logger = KotlinLogging.logger {}

@Component
class SqsEventPublisher : IEventPublisherBase {


    @Autowired
    private lateinit var sqsConfiguration: SqsConfiguration

    @Autowired
    private lateinit var tabourConfigs: TabourAutoConfigs

    override fun getType(): BusType {
        return BusType.SQS
    }

    override fun <T : Serializable> publish(message: T, busUrl: String) {
        publish(message, busUrl, SendMessageRequest())
    }

    /**
     * Publishes a message with a pre-configured [SendMessageRequest] which gives you all the
     * options you may need from the underlying SQS client. Note that the `queueUrl` and `messageBody`
     * must not be set because they will be set by the this publisher.
     */
    private fun <T : Serializable> publish(message: T, busUrl: String, preConfiguredRequest: SendMessageRequest) {
        require(preConfiguredRequest.queueUrl == null) { "attribute queueUrl of pre-configured request must not be set!" }
        require(preConfiguredRequest.messageBody == null) { "message body of pre-configured request must not be set!" }
        val retry = tabourConfigs.retryRegistry().retry("publish")
        retry
            .eventPublisher
            .onError {
                logger.warn(
                    "error publishing message to queue {}",
                    busUrl
                )
            }
        retry.executeRunnable { doPublish(message, busUrl, preConfiguredRequest) }
    }

    private fun <T : Serializable> doPublish(message: T, busUrl: String, preConfiguredRequest: SendMessageRequest) {
        logger.debug(
            "sending message {} to SQS queue {}", message.toString(), busUrl
        )
        val request = preConfiguredRequest
            .withQueueUrl(busUrl)
            .withMessageBody(message.toString())
        val result = sqsConfiguration.amazonSQSAsync().sendMessage(request)
        if (result.sdkHttpMetadata.httpStatusCode != 200) {
            throw RuntimeException(
                String.format(
                    "got error response from SQS queue %s: %s",
                    busUrl, result.sdkHttpMetadata
                )
            )
        }
        logger.info("Sent message ID {}", result.messageId)
    }

}
