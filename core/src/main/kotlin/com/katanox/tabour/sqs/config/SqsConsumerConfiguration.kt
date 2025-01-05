package com.katanox.tabour.sqs.config

import com.katanox.tabour.consumption.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SqsConsumerConfiguration internal constructor() : Config {

    /** The number of max messages to fetch. Default is 10 which is also the max numbers allowed */
    var maxMessages: Int = 10
        @Throws(IllegalArgumentException::class)
        set(value) {
            require(value < 10 || value > 0)
            field = value
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
            require(value <= 50 || value > 0)
            field = value
        }

    /**
     * The delay between subsequent requests
     *
     * Default is 10 seconds
     */
    var sleepTime: Duration = 10.seconds

    /**
     * The duration for which the call waits for a message to arrive in the queue before returning.
     * If a message is available, the call returns sooner than WaitTimeSeconds. If no messages are
     * available and the wait time expires, the call returns successfully with an empty list of
     * messages.
     *
     * Default is 0 seconds
     */
    var waitTime: Duration = 0.seconds

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
