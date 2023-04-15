package com.katanox.tabour.config

import config.SqsConfiguration

object Configuration {
    fun sqs(init: SqsConfiguration.() -> Unit): SqsConfiguration {
        val s = SqsConfiguration()
        s.init()

        return s
    }
}
