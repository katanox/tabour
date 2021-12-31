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
)
