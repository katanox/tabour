package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.production.SqsProducer
import software.amazon.awssdk.services.sqs.model.Message

class SqsPipeline internal constructor() : Config {

    var producer: SqsProducer = sqsProducer {}

    var transformer: (Message) -> String? = { null }
}
