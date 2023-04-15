package com.katanox.tabour.config

object Configuration {
    fun sqs(init: SqsConfiguration.() -> Unit): SqsConfiguration {
        val s = SqsConfiguration()
        s.init()

        return s
    }
}
