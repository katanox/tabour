package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.production.SqsProducer
import software.amazon.awssdk.services.sqs.model.Message

class SqsPipeline : Config {
    internal var transformerWasSet = false
    internal var producerWasSet = false

    var producer: SqsProducer = sqsProducer {}
        set(value) {
            producerWasSet = value.urlWasSet()
            field = value
        }

    var transformer: (Message) -> String = { "" }
        set(value) {
            field = value
            transformerWasSet = true
        }
}
