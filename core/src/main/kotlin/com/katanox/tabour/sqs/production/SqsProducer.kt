package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import com.katanox.tabour.configuration.sqs.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import java.net.URL

class SqsProducer<K>
internal constructor(
    /**
     * [key] must be unique for each producer. It will be used to select the correct producer when
     * producing a message through a Registry
     */
    override val key: K,
    val queueUrl: URL,
    override val onError: suspend (ProductionError) -> Unit,
) : Config, TabourProducer<K> {
    var config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
}

sealed class SqsProductionData {
    data class Single(val builder: (SendMessageRequest.Builder.() -> Unit)) : SqsProductionData()

    data class Batch(val builder: (SendMessageBatchRequest.Builder.() -> Unit)) :
        SqsProductionData()
}
