package com.katanox.tabour.sqs.config

import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import software.amazon.awssdk.services.sqs.model.Message

class SqsConsumer internal constructor(val config: SqsConsumerConfiguration) :
    Consumer<Message, ConsumptionError>, Config {

    override var onSuccess: suspend (Message) -> Boolean = { false }

    override var onError: (ConsumptionError) -> Unit = {}

    var pipeline: SqsPipeline? = null
}
