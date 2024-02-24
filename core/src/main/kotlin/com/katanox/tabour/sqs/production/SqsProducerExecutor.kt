package com.katanox.tabour.sqs.production

import com.katanox.tabour.plug.FailurePlugRecord
import com.katanox.tabour.plug.SuccessPlugRecord
import com.katanox.tabour.retry
import java.time.Instant
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

internal class SqsProducerExecutor(private val sqs: SqsClient) {
    suspend fun <T> produce(
        producer: SqsProducer<T>,
        productionConfiguration: SqsDataProductionConfiguration
    ) {
        val produceData = productionConfiguration.produceData()

        val url = producer.queueUrl.toString()

        if (!produceData.message.isNullOrEmpty() && url.isNotEmpty()) {
            retry(
                producer.config.retries,
                {
                    val error =
                        when (it) {
                            is AwsServiceException ->
                                ProductionError.AwsError(details = it.awsErrorDetails())
                            is SdkClientException -> ProductionError.AwsSdkClientError(it)
                            else -> ProductionError.UnrecognizedError(it)
                        }

                    producer.onError(error)
                    producer.notifyPlugs(produceData.message, error)
                }
            ) {
                val response =
                    sqs.sendMessage {
                        produceData.buildMessageRequest(it)
                        it.queueUrl(url)
                    }

                if (response.messageId().isNotEmpty()) {
                    productionConfiguration.dataProduced(
                        produceData,
                        SqsMessageProduced(response.messageId(), Instant.now())
                    )
                }

                producer.notifyPlugs(produceData.message)
            }
        } else {
            val error =
                when {
                    url.isEmpty() -> ProductionError.EmptyUrl(producer.queueUrl)
                    produceData.message.isNullOrEmpty() -> ProductionError.EmptyMessage(produceData)
                    else -> null
                }

            if (error != null) {
                producer.notifyPlugs(produceData.message, error)
            }
        }
    }

    private suspend fun <T> SqsProducer<T>.notifyPlugs(
        message: String?,
        error: ProductionError? = null
    ) {
        if (plugs.isNotEmpty()) {
            plugs.forEach { plug ->
                if (error == null) {
                    plug.handle(SuccessPlugRecord(message, key))
                } else {
                    plug.handle(FailurePlugRecord(message, key, error))
                }
            }
        }
    }
}

private fun SqsDataForProduction.buildMessageRequest(builder: SendMessageRequest.Builder) {

    when (this) {
        is FifoQueueData -> {
            builder.messageBody(message)
            builder.messageGroupId(messageGroupId)

            if (messageDeduplicationId != null) {
                builder.messageDeduplicationId(messageDeduplicationId)
            }
        }
        is NonFifoQueueData -> builder.messageBody(message)
    }
}
