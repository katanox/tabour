package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import com.katanox.tabour.retry
import java.time.Instant
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException

internal class SqsProducerExecutor(private val sqs: SqsClient) {
    private fun throwableToError(e: Throwable): ProductionError =
        when (e) {
            is AwsServiceException -> ProductionError.AwsError(details = e.awsErrorDetails())
            is SdkClientException -> ProductionError.AwsSdkClientError(e)
            else -> ProductionError.UnrecognizedError(e)
        }

    private val chunkSize = 10

    suspend fun <T> produce(
        producer: SqsProducer<T>,
        productionConfiguration: SqsDataProductionConfiguration,
    ) {
        val produceData = productionConfiguration.produceData()

        val url = producer.queueUrl.toString()

        if (url.isEmpty()) {
            producer.onError(ProductionError.EmptyUrl(producer.queueUrl))
            return
        }

        when (produceData) {
            is SqsProductionData -> {
                if (!produceData.message.isNullOrEmpty()) {
                    retry(producer.config.retries, { producer.onError(throwableToError(it)) }) {
                        val request = SendMessageRequest { produceData.buildMessageRequest(this) }

                        sqs.sendMessage(request).let { response ->
                            val messageId = response.messageId
                            if (messageId != null && messageId.isNotEmpty()) {
                                productionConfiguration.dataProduced?.invoke(
                                    produceData,
                                    SqsMessageProduced(messageId, Instant.now()),
                                )
                            }
                        }
                    }
                } else {
                    if (produceData.message.isNullOrEmpty()) {
                        producer.onError(ProductionError.EmptyMessage(produceData))
                    }
                }
            }
            is BatchDataForProduction -> {
                if (produceData.data.isNotEmpty()) {
                    produceData.data
                        .chunked(chunkSize) {
                            SendMessageBatchRequest {
                                queueUrl = url
                                entries = buildBatchGroup(it)
                            }
                        }
                        .forEach { request ->
                            retry(
                                producer.config.retries,
                                { producer.onError(throwableToError(it)) },
                            ) {
                                val response = sqs.sendMessageBatch(request)

                                if (response.failed.isEmpty()) {
                                    response.successful.zip(produceData.data).forEach {
                                        (entry, data) ->
                                        productionConfiguration.dataProduced?.invoke(
                                            data,
                                            SqsMessageProduced(entry.messageId, Instant.now()),
                                        )
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}

private fun SqsProductionData.buildMessageRequest(builder: SendMessageRequest.Builder) {
    when (this) {
        is FifoDataProduction -> {
            builder.messageBody = message
            builder.messageGroupId = messageGroupId

            if (messageDeduplicationId != null) {
                builder.messageDeduplicationId = messageDeduplicationId
            }
        }
        is NonFifoDataProduction -> builder.messageBody = message
    }
}

private fun SqsProductionData.buildMessageRequest(builder: SendMessageBatchRequestEntry.Builder) {
    when (this) {
        is FifoDataProduction -> {
            builder.messageBody = message
            builder.messageGroupId = messageGroupId

            if (messageDeduplicationId != null) {
                builder.messageDeduplicationId = messageDeduplicationId
            }
        }
        is NonFifoDataProduction -> builder.messageBody = message
    }
}

private fun buildBatchGroup(data: List<SqsProductionData>): List<SendMessageBatchRequestEntry> =
    data.mapIndexed { index, entry ->
        SendMessageBatchRequestEntry {
            entry.buildMessageRequest(this)
            id = (index + 1).toString()
        }
    }
