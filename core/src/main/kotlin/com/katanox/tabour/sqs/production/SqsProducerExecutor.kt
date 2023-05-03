package com.katanox.tabour.sqs.production

import com.katanox.tabour.retry
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

internal class SqsProducerExecutor(private val sqs: SqsAsyncClient) {
    suspend fun produce(producer: SqsProducer, produceFn: () -> String) {
        val request =
            SendMessageRequest.builder()
                .messageBody(produceFn())
                .queueUrl(producer.queueUrl.toASCIIString())
                .build()

        retry(
            producer.config.retries,
            {
                producer.onError(
                    ProducerError(
                        it.message
                            ?: "Unknown exception during production (producer key: [${producer.key}])"
                    )
                )
            }
        ) {
            sqs.sendMessage(request).await()
        }
    }
}
