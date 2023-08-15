package com.katanox.tabour.sqs.config

import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.production.SqsProducer
import software.amazon.awssdk.services.sqs.model.Message

class SqsPipeline internal constructor() : Config {
    var producer: SqsProducer<*>? = null

    var transformer: (Message) -> Pair<String?, String> = { Pair(null, "") }
}
