package com.katanox.tabour.sqs.production

import com.katanox.tabour.consumption.Config

class SqsProducerConfiguration internal constructor() : Config {
    var retries: Int = 1
}