package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqs.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import java.net.URI

class SqsProducer<K> internal constructor(
    /**
     * [key] must be unique for each producer. It will be used to select the correct producer when
     * producing a message through a Registry
     */
    override val key: K
) : Config, TabourProducer<K> {
    var queueUrl: URI = URI("")

    override val onError: (ProducerError<K>) -> Unit = {}

    var config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
}
