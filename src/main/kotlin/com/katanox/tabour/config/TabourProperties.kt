package com.katanox.tabour.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@ConfigurationProperties(prefix = "tabour")
@Component
data class TabourProperties(

    /**
     *
     * Number of times that is gonna retry to publish  or consume an event
     *
     * The default is 1.
     */
    var retryMaxCount: Int = 1
)