package com.katanox.tabour.sqs.production

import com.katanox.tabour.retry
import java.time.Instant
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

internal class SqsProducerExecutor(private val sqs: SqsClient) {
    suspend fun <T> produce(producer: SqsProducer<T>, f: SqsDataProductionConfiguration) {
        val produceData = f.produceData()

        val url = producer.queueUrl.toString()

        if (!produceData.message.isNullOrEmpty() && url.isNotEmpty()) {
            val request =
                SendMessageRequest.builder()
                    .queueUrl(url)
                    .apply {
                        when (produceData) {
                            is FifoQueueData -> {
                                this.messageBody(produceData.message)
                                this.messageGroupId(produceData.messageGroupId)
                            }
                            is NonFifoQueueData -> this.messageBody(produceData.message)
                        }
                    }
                    .build()

            retry(
                producer.config.retries,
                {
                    when (it) {
                        is AwsServiceException ->
                            producer.onError(
                                ProductionError.AwsError(details = it.awsErrorDetails())
                            )
                        is SdkClientException ->
                            producer.onError(ProductionError.AwsSdkClientError(it))
                        else -> producer.onError(ProductionError.UnrecognizedError(it))
                    }
                }
            ) {
                val response = sqs.sendMessage(request)

                if (response.messageId().isNotEmpty()) {
                    f.dataProduced(
                        produceData,
                        SqsMessageProduced(response.messageId(), Instant.now())
                    )
                }
            }
        } else {
            when {
                url.isEmpty() -> producer.onError(ProductionError.EmptyUrl(producer.queueUrl))
                produceData.message.isNullOrEmpty() ->
                    producer.onError(ProductionError.EmptyMessage(produceData))
            }
        }
    }
}
