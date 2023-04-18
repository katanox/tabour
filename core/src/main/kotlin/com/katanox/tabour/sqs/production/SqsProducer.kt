package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import java.net.URI
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

interface TabourProducer {
    val produce: () -> String
}

class SqsProducer internal constructor() : Config, TabourProducer {
    var queueUrl: URI = URI("")
    var key: String = ""
    val config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
    override var produce: () -> String = { "" }
}

class SqsProducerConfiguration internal constructor() : Config {
    var retries: Int = 1
}

internal class SqsProd(private val sqs: SqsAsyncClient) {
    suspend fun produce(sqsProducer: SqsProducer) {
        val request =
            SendMessageRequest.builder()
                .messageBody(sqsProducer.produce())
                .queueUrl(sqsProducer.queueUrl.toASCIIString())
                .build()

        repeat(sqsProducer.config.retries) { sqs.sendMessage(request).await() }
    }
}
