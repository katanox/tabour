package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqs.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import java.net.URI

class SqsProducer internal constructor() : Config, TabourProducer {
    var queueUrl: URI = URI("")

    override val onError: (ProducerError) -> Unit = {}

    fun urlWasSet() = queueUrl.toASCIIString().isNotEmpty()

    /**
     * [key] must be unique for each producer. It will be used to select the correct producer when
     * producing a message through a Registry
     */
    override var key: String = ""
    var config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
}
