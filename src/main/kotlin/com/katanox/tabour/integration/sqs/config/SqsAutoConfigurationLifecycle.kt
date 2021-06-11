package com.katanox.tabour.integration.sqs.config

import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandlerRegistry
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import javax.annotation.PreDestroy


class SqsAutoConfigurationLifecycle(
    private val registry: SqsEventHandlerRegistry
) : ApplicationListener<ApplicationReadyEvent> {


    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        registry.start()
    }

    @PreDestroy
    fun destroy() {
        registry.stop()
    }

}