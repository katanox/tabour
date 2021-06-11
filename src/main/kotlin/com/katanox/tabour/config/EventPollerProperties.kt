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
     * The default is 1 threads.
     */
    var pollDelay: Duration = Duration.of(1, ChronoUnit.SECONDS),
    /**
     * The duration the should wait for messages before closing the connection.
     *
     *
     * The default is 20 second.
     */
    var waitTime: Duration = Duration.ofSeconds(20),
    /**
     * Visibility timeout is the time-period or duration you specify for the queue item which when is fetched
     * and processed by the consumer is made hidden from the queue and other consumers.
     *
     *
     * The default is 360 second.
     */
    var visibilityTimeout: Duration = Duration.ofSeconds(360),
    /**
     * The maximum number of messages to pull from the even bus each poll
     *  SQSl: SQS allows is maximum 10
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