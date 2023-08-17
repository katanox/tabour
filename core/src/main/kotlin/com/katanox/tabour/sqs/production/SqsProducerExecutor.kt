package com.katanox.tabour.sqs.production

import com.katanox.tabour.retry
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

internal class SqsProducerExecutor(private val sqs: SqsAsyncClient) {
    suspend fun <T> produce(producer: SqsProducer<T>, produceFn: () -> Pair<String?, String>) {
        val (body, messageGroupId) = produceFn()
        val url = producer.queueUri.toString()

        if (!body.isNullOrEmpty() && url.isNotEmpty()) {
            val request =
                SendMessageRequest.builder()
                    .messageBody(body)
                    .queueUrl(url)
                    .messageGroupId(messageGroupId)
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
                sqs.sendMessage(request).await()
            }
        }
    }
}
