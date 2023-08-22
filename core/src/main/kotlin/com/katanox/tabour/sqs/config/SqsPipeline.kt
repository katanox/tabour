package com.katanox.tabour.sqs.config

import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.production.NonFifoQueueData
import com.katanox.tabour.sqs.production.SqsDataForProduction
import com.katanox.tabour.sqs.production.SqsProducer
import software.amazon.awssdk.services.sqs.model.Message

class SqsPipeline internal constructor() : Config {
    var producer: SqsProducer<*>? = null

    /**
     * A function that returns a new body to be produced with its message group id based on a
     * consumed sqs message
     */
    var transformer: (Message) -> SqsDataForProduction = { NonFifoQueueData(message = null) }
}
