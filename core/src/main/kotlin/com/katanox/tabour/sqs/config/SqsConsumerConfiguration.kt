package com.katanox.tabour.sqs.config

import com.katanox.tabour.consumption.Config
import java.time.Duration
import java.time.temporal.ChronoUnit

class SqsConsumerConfiguration internal constructor() : Config {
    /** The number of max messages to fetch. Default is 1 with max being 10 */
    var maxMessages: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }

    /**
     * Unit of concurrency. In combination with [maxMessages] determines the max number of messages
     * that can be received every [sleepTime]. The upper bound of messages every [sleepTime] is
     * [maxMessages] * [concurrency]
     */
    var concurrency: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }
    /**
     * The delay between subsequent requests
     *
     * Default is 10 seconds
     */
    var sleepTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)

    /**
     * The duration for which the call waits for a message to arrive in the queue before returning.
     * If a message is available, the call returns sooner than WaitTimeSeconds. If no messages are
     * available and the wait time expires, the call returns successfully with an empty list of
     * messages.
     *
     * Default is 10 seconds
     */
    var waitTime: Duration = Duration.of(0L, ChronoUnit.SECONDS)

    /**
     * The number of attempts to receive a message if an exception occurs
     *
     * Default is 1
     */
    var retries: Int = 1
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Negative values are not allowed")
            }
            field = value
        }

    var consumeWhile: () -> Boolean = { true }
}
