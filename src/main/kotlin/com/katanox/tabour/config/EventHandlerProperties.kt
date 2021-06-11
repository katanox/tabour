package com.katanox.tabour.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "tabour.handler")
@Component
data class EventHandlerProperties(
    /**
     *
     * Size of the thread pool the the handler actors will use to consume the messages
     *
     * The default is 3 threads.
     */
    var threadPoolSize: Int = 3,

    /**
     *
     * Size of the internal queue that the threads will use to pull from once a message is consumed
     *
     * The default is 10.
     */
    var queueSize: Int = 10,
    /**
     * The prefix of the threads that are created by the thread pool
     *
     *
     * The default is empty string.
     */
    var threadNamePrefix: String = ""
) {


}