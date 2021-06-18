package com.katanox.tabour.config

import com.katanox.tabour.exception.ExceptionHandler
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.temporal.ChronoUnit


@ConfigurationProperties(prefix = "tabour.poller")
@Component
data class EventPollerProperties(

    /**
     * The delay the poller should wait for the next poll after the previous poll has finished
     *
     * The default is 1 second.
     */
    var pollDelay: Duration = Duration.of(1, ChronoUnit.SECONDS),

    /**
     * The duration (in seconds) for which the call waits for a message to arrive in the queue before returning.
     * If a message is available, the call returns sooner than WaitTimeSeconds.
     * If no messages are available and the wait time expires, the call returns successfully with an empty list of messages.
     *
     * The default is 20 second.
     */
    var waitTime: Duration = Duration.ofSeconds(20),

    /**
     * Visibility timeout is the time-period for which the queue item is hidden from other consumers after being fetched
     *
     *
     * The default is 360 second.
     */
    var visibilityTimeout: Duration = Duration.ofSeconds(360),

    /**
     * The maximum number of messages to pull from the even bus each poll
     *  event bus:
     *      - SQS allows a maximum of 10
     *
     *
     * The default is 10.
     */
    var batchSize: Int = 10,

    /**
     * The number of threads that should poll for new messages. Each of those threads will poll a
     * batch of batchSize messages and then wait for the pollDelay interval until polling the next
     * batch.
     *
     *
     * The default is 1.
     */
    var pollingThreads: Int = 1,


    ) {
    var exceptionHandler: ExceptionHandler = ExceptionHandler.defaultExceptionHandler()
}