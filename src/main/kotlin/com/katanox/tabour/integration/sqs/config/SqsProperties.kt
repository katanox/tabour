package com.katanox.tabour.integration.sqs.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "tabour.sqs")
@Component
data class SqsProperties(
    /**
     *
     * The AWS access key.
     */
    var accessKey: String = "",

    /**
     *
     * The AWS secret access key.
     */
    var secretKey: String = "",

    /**
     *
     * Sets the region to be used by the client. This will be used to determine both the service
     * endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1) for
     * requests.
     */
    var region: String = "",

    /**
     *
     * Configures if this listening container should be automatically started.
     *
     * The default is true
     */
    var autoStartup: Boolean = true,

    /**
     * Configure the maximum number of messages that should be retrieved during one poll to the
     * Amazon SQS system. This number must be a positive, non-zero number that has a maximum number
     * of 10. Values higher then 10 are currently not supported by the queueing system. the maximum
     * number of messages (between 1-10)
     *
     * The default is 10
     */
    var maxNumberOfMessages: Int = 10,

    /**
     * Set the ThreadPoolExecutor's core pool size, that is being used by SQS
     *
     * Default is 1.
     */
    var corePoolSize: Int = 1,

    /**
     * Set the ThreadPoolExecutor's maximum pool size, that is being used by SQS
     *
     * Default is Integer.MAX_VALUE.
     */
    var maxPoolSize: Int = Int.MAX_VALUE,

    /**
     * Set the capacity for the ThreadPoolExecutor's BlockingQueue, that is being used by SQS Any
     * positive value will lead to a LinkedBlockingQueue instance; Any other value will lead to a
     * SynchronousQueue instance
     *
     * Default is Integer.MAX_VALUE.
     */
    var queueCapacity: Int = 100,

    /**
     * Configures if this the sqs listeners should be starting
     *
     * The default is false
     */
    var enableConsumption: Boolean = false,
)
