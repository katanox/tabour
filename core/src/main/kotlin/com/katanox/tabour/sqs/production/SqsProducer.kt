package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqs.sqsProducerConfiguration
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.plug.ProducerPlug
import java.net.URL

class SqsProducer<K>
internal constructor(
    /**
     * [key] must be unique for each producer. It will be used to select the correct producer when
     * producing a message through a Registry
     */
    override val key: K,
    val queueUrl: URL
) : Config, TabourProducer<K> {

    override var onError: (ProductionError) -> Unit = {}

    override var plugs: MutableList<ProducerPlug> = mutableListOf()

    var config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
}

sealed interface SqsDataForProduction {
    val message: String?
}

data class FifoQueueData(override val message: String?, val messageGroupId: String = "") :
    SqsDataForProduction

data class NonFifoQueueData(override val message: String?) : SqsDataForProduction
