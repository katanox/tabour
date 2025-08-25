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

    /**
     * Unit of concurrency. In combination with [maxMessages] determines the max number of messages
     * that can be received every [sleepTime]. The upper bound of messages every [sleepTime] is
     * [maxMessages] * [concurrency]
     *
     * Max coroutines: 50
     */
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

    /**
     * The number of attempts to receive a message if an exception occurs
     *
     * Default is 1
     */
    var retries: Int = 1
        @Throws(IllegalArgumentException::class)
        set(value) {
            require(value > 0)
            field = value
        }

    /**
     * Used to dynamically enable or disable a consumer.
     *
     * Default is true, which means that the consumer will start normally by default
     */
    var consumeWhile: () -> Boolean = { true }
}
