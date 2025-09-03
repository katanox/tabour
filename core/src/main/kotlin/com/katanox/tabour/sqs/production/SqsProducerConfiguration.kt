package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.core.DataProductionConfiguration
import com.katanox.tabour.consumption.Config

class SqsProducerConfiguration internal constructor() : Config {
    /** How many times the producer will try to produce a message */
    var retries: Int = 1
}

typealias SqsDataProductionConfiguration = DataProductionConfiguration<SqsProductionData>
