package com.katanox.tabour.sqs.production

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

internal class SqsProducerExecutor(private val sqs: SqsAsyncClient) {
    suspend fun produce(producer: SqsProducer) {
        if (producer.queueUrl.toASCIIString().isNotBlank()) {
            val request =
                SendMessageRequest.builder()
                    .messageBody(producer.produce())
                    .queueUrl(producer.queueUrl.toASCIIString())
                    .build()

            repeat(producer.config.retries) { sqs.sendMessage(request).await() }
        }
    }
}
