package com.katanox.tabour.sqs.config

import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.katanox.tabour.consumption.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SqsConsumerConfiguration internal constructor() : Config {

    /** A lambda function that is used to configure the receive request of the consumer */
    var receiveRequestConfigurationBuilder: (ReceiveMessageRequest.Builder.() -> Unit) = {
        maxNumberOfMessages = 10
    }

    var concurrency: Int = 1
        @Throws(IllegalArgumentException::class)
        set(value) {
            require(value in 0..50)
            field = value
        }

    /**
     * The delay between subsequent requests
     *
     * Default is 10 seconds
     */
    var sleepTime: Duration = 10.seconds
}
