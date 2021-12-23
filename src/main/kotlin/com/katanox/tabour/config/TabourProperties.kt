package com.katanox.tabour.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "tabour")
@Component
data class TabourProperties(

    /**
     *
     * Number of times that is going to retry to publish or consume an event
     *
     * The default is 3.
     */
    var maxRetryCount: Int = 3,
)
