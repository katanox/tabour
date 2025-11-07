package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import com.katanox.tabour.consumption.Config
import java.net.URL

/**
 * Used to create producers that will be registered to a [com.katanox.tabour.sqs.SqsRegistry] in
 * order to produce messages. An instance of it can be created using
 * [com.katanox.tabour.configuration.sqs.sqsProducer]
 */
class SqsProducer<K>
internal constructor(
    /**
     * [key] must be unique for each producer. It will be used to select the correct producer when
     * producing a message through a Registry
     */
    val key: K,
    /** The sqs queue url where the message should be produced to */
    val queueUrl: URL,
    /** The handler which will be called in any failure while producing the message */
    val onError: suspend (ProductionError) -> Unit,
) : Config

sealed class SqsProductionData {
    /** Used to produce a single message */
    data class Single(val builder: (SendMessageRequest.Builder.() -> Unit)) : SqsProductionData()

    /** Used to produce a batch message */
    data class Batch(val builder: (SendMessageBatchRequest.Builder.() -> Unit)) :
        SqsProductionData()
}
