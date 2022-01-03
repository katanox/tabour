package com.katanox.tabour.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@ConfigurationProperties(prefix = "tabour.poller")
@Component
data class EventPollerProperties(

    /**
     * The delay the poller should wait for the next poll after the previous poll has finished
     *
     * The default is null.
     */
    var pollDelay: Duration? = null,

    /**
     * The duration (in seconds) for which the call waits for a message to arrive in the queue
     * before returning. If a message is available, the call returns sooner than WaitTimeSeconds. If
     * no messages are available and the wait time expires, the call returns successfully with an
     * empty list of messages.
     *
     * The default is 20 second.
     */
    var waitTime: Duration = Duration.ofSeconds(DEFAULT_WAIT_TIME),

    /**
     * Visibility timeout is the time-period for which the queue item is hidden from other consumers
     * after being fetched
     *
     * The default is 360 second.
     */
    var visibilityTimeout: Duration = Duration.ofSeconds(DEFAULT_VISIBILITY_TIMEOUT),

    /**
     * The maximum number of messages to pull from the even bus each poll event bus:
     * ```
     *      - SQS allows a maximum of 10
     * ```
     * The default is 10.
     */
    var batchSize: Int = DEFAULT_BATCH_SIZE,

    /**
     * The number of coroutines that should poll for new messages and then consume them.
     *
     * The default is 10.
     */
    var numOfPollers: Int = 10,
) {

    companion object {
        const val DEFAULT_VISIBILITY_TIMEOUT = 360L
        const val DEFAULT_WAIT_TIME = 20L
        const val DEFAULT_BATCH_SIZE = 10
    }
}
