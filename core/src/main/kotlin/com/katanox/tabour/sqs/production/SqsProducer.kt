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
    val queueUrl: URL,
    override val onError: suspend (ProductionError) -> Unit
) : Config, TabourProducer<K> {

    /**
     * This function is invoked if the message is not successfully produced after
     * [SqsProducerConfiguration.retries] times
     */
    override var plugs: MutableList<ProducerPlug> = mutableListOf()

    var config: SqsProducerConfiguration = sqsProducerConfiguration { retries = 1 }
}

sealed interface SqsDataForProduction

/**
 * SQS producers use instances of this interfaces in order to produce messages to queues.
 * - For FIFO queues use [FifoDataProduction]
 * - For Non FIFO queues use [NonFifoDataProduction]
 */
sealed interface SqsProductionData : SqsDataForProduction {
    val message: String?
}

data class FifoDataProduction(
    override val message: String?,
    val messageGroupId: String,
    val messageDeduplicationId: String? = null
) : SqsProductionData

data class NonFifoDataProduction(override val message: String?) : SqsProductionData

data class BatchDataForProduction(val data: List<SqsProductionData>) : SqsDataForProduction
