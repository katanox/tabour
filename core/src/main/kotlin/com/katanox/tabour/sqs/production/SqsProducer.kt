package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import java.net.URI

interface TabourProducer {
    val produce: () -> String
}

class SqsProducer internal constructor() : Config, TabourProducer {
    var queueUrl: URI = URI("")
        set(value) {
            if (value.toASCIIString().isBlank()) {
                throw IllegalArgumentException("URL can not be empty")
            }
            field = value
        }

    /**
     * [key] must be unique for each producer. It will be used to select the correct producer
     * when producing a message through a Registry
     */
    var key: String = ""
    val config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
    override var produce: () -> String = { "" }
}

class SqsProducerConfiguration internal constructor() : Config {
    var retries: Int = 1
}

