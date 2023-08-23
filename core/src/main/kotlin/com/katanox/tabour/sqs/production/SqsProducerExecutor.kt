package com.katanox.tabour.sqs.production

import com.katanox.tabour.retry
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

internal class SqsProducerExecutor(private val sqs: SqsClient) {
    suspend fun <T> produce(producer: SqsProducer<T>, produceFn: () -> SqsDataForProduction) {
        val produceData = produceFn()

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
                    producer.onError(
                        ProducerError(
                            producerKey = producer.key,
                            message = it.message
                                    ?: "Unknown exception during production (producer key: [${producer.key}])"
                        )
                    )
                }
            ) {
                sqs.sendMessage(request)
            }
        }
    }
}
