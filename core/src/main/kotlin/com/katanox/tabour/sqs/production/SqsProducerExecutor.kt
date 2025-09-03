package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.ServiceException

internal class SqsProducerExecutor {
    suspend fun <T> produce(
        sqsClient: SqsClient,
        producer: SqsProducer<T>,
        productionConfiguration: SqsDataProductionConfiguration,
    ) {
        val produceData = productionConfiguration.produceData()

        val url = producer.queueUrl.toString()

        if (url.isEmpty()) {
            producer.onError(ProductionError.EmptyUrl(producer.queueUrl))
            return
        }

        try {
            when (produceData) {
                is SqsProductionData.Single -> {
                    val request = SendMessageRequest {
                        produceData.builder(this)
                        queueUrl = url
                    }

                    sqsClient.sendMessage(request).let { response ->
                        val messageId = response.messageId
                        if (messageId == null || messageId.isEmpty()) {
                            producer.onError(ProductionError.EmptyMessageId)
                        }
                    }
                }
                is SqsProductionData.Batch -> {
                    val request = SendMessageBatchRequest {
                        produceData.builder(this)
                        queueUrl = url
                    }

                    val response = sqsClient.sendMessageBatch(request)

                    if (response.failed.isNotEmpty()) {
                        producer.onError(ProductionError.FailedBatch(response.failed))
                    }
                }
            }
        } catch (e: ClientException) {
            producer.onError(throwableToError(e))
        } catch (e: ServiceException) {
            producer.onError(throwableToError(e))
        }
    }
}

private fun throwableToError(e: Throwable): ProductionError =
    when (e) {
        is ServiceException -> ProductionError.AwsServiceError(exception = e)
        is ClientException -> ProductionError.AwsClientError(e)
        else -> ProductionError.UnrecognizedError(e)
    }
