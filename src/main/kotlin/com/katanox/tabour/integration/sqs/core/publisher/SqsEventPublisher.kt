package com.katanox.tabour.integration.sqs.core.publisher

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry

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

    override fun publish(message: String, busUrl: String, messageGroupId: String?) {
        publishBatch(listOf(message), busUrl, messageGroupId)
    }

    override fun publishBatch(messages: List<String>, busUrl: String, messagesGroupId: String?) {
        publish(messages, busUrl, messagesGroupId)
    }

    /**
     * Publishes a message with a pre-configured [SendMessageRequest] which gives you all the options
     * you may need from the underlying SQS client. Note that the `queueUrl` and `messageBody` must
     * not be set because they will be set by the publisher.
     */
    private fun publish(
        messages: List<String>,
        busUrl: String,
        messageGroupId: String?,
    ) {

        val retry = tabourConfigs.retryRegistry().retry("publish")
        retry.eventPublisher.onError { logger.warn("error publishing message to queue {}", busUrl) }
        retry.executeRunnable { doPublish(messages, busUrl, messageGroupId) }
    }

    private fun doPublish(
        messages: List<String>,
        busUrl: String,
        messageGroupId: String?,
    ) {
        runBlocking {
            messages.chunked(10).forEach {
                launch {
                    logger.debug("sending message {} to SQS queue {}", it, busUrl)
                    val request = SendMessageBatchRequest.builder().queueUrl(busUrl)
                        .entries(
                            it.mapIndexed { index, message ->
                                val entry =
                                    SendMessageBatchRequestEntry.builder().id(index.toString()).messageBody(message)
                                messageGroupId?.let {
                                    entry.messageGroupId(it)
                                }
                                entry.build()
                            }
                        ).build()
                    require(request.queueUrl() == null) {
                        "attribute queueUrl of pre-configured request must not be set!"
                    }
                    require(request.hasEntries()) {
                        "message body of pre-configured request must not be set!"
                    }
                    val result = sqsConfiguration.amazonSQSAsync().sendMessageBatch(request).await()
                    if (result.sdkHttpResponse().statusCode() != HttpStatus.SC_OK) {
                        throw RuntimeException(
                            String.format("got error response from SQS queue %s: %s", busUrl, result.responseMetadata())
                        )
                    }
                    logger.info("Sent message ID {}", result.successful().size)
                }
            }
        }
    }
}
