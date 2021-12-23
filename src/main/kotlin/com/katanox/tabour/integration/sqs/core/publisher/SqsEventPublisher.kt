package com.katanox.tabour.integration.sqs.core.publisher

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.extentions.retry
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
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
     * Publishes a message with [SendMessageBatchRequest] which gives you all the options
     * you may need from the underlying SQS client. Note that the `queueUrl` and `messageBody` must
     * not be set because they will be set by the publisher.
     */
    private fun publish(
        messages: List<String>,
        busUrl: String,
        messageGroupId: String?,
    ) {
        runBlocking {
            retry(times = tabourConfigs.tabourProperties.maxRetryCount) {
                doPublish(
                    messages,
                    busUrl,
                    messageGroupId
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun doPublish(
        messages: List<String>,
        busUrl: String,
        messagesGroupId: String?,
    ) {
        GlobalScope.launch {
            messages.chunked(messages.size / sqsConfiguration.sqsProperties.publishingCoroutines).forEach {
                async {
                    publishChunk(it, busUrl, messagesGroupId)
                }.start()
            }
        }
    }

    private fun publishChunk(
        messages: List<String>,
        busUrl: String,
        messageGroupId: String?,
    ) {
        messages.chunked(10).parallelStream().forEach { messageChunk ->
            logger.debug("sending messages chunk $messages to SQS queue $busUrl")
            val request = prepareRequest(busUrl, messageChunk, messageGroupId)
            validateRequest(request)
            val result = sqsConfiguration.amazonSQSAsync().sendMessageBatch(request)
            if (result.sdkHttpResponse().statusCode() != HttpStatus.SC_OK) {
                throw RuntimeException(
                    "got error response from SQS queue $busUrl: ${result.responseMetadata()}"
                )
            }
            logger.debug(
                "Sent messages  with IDs ${result.successful().joinToString(",") { it.messageId() }}"
            )
        }
    }

    private fun validateRequest(request: SendMessageBatchRequest) {
        require(request.queueUrl() != null) {
            "attribute queueUrl of pre-configured request must not be set!"
        }
        require(request.hasEntries()) {
            "message body of pre-configured request must not be set!"
        }
    }

    private fun prepareRequest(
        busUrl: String,
        messageChunk: List<String>,
        messageGroupId: String?,
    ) = SendMessageBatchRequest.builder().queueUrl(busUrl)
        .entries(
            messageChunk.mapIndexed { index, message ->
                val entry =
                    SendMessageBatchRequestEntry.builder().id(index.toString()).messageBody(message)
                messageGroupId?.let {
                    entry.messageGroupId(messageGroupId)
                }
                entry.build()
            }
        ).build()
}
