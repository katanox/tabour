package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqs.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import java.net.URI

interface TabourProducer {
    val key: String
}

class SqsProducer internal constructor() : Config, TabourProducer {
    internal var urlWasSet: Boolean = false
    var queueUrl: URI = URI("")
        set(value) {
            if (value.toASCIIString().isBlank()) {
                throw IllegalArgumentException("URL can not be empty")
            }
            urlWasSet = true
            field = value
        }

    /**
     * [key] must be unique for each producer. It will be used to select the correct producer when
     * producing a message through a Registry
     */
    override var key: String = ""
    var config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
}

class SqsProducerConfiguration internal constructor() : Config {
    var retries: Int = 1
}
