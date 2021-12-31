package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SqsEventHandlerRegistry(
    eventHandlers: List<SqsEventHandler>,
    private val sqsConfiguration: SqsConfiguration,
    private val tabourAutoConfigs: TabourAutoConfigs,
    private val pollerConfigs: EventPollerProperties,
) {
    private var pollers: Set<SqsEventPoller> = setOf()

    init {
        pollers = initializePollers(eventHandlers)
    }

    private fun initializePollers(registrations: List<SqsEventHandler>): Set<SqsEventPoller> {
        val pollers: MutableSet<SqsEventPoller> = HashSet()
        for (registration in registrations) {
            pollers.add(createPollerForHandler(registration))
            logger.info("initialized SqsMessagePoller ${registration.javaClass.canonicalName}")
        }
        return pollers
    }

    private fun createPollerForHandler(registration: SqsEventHandler): SqsEventPoller {
        return SqsEventPoller(
            queueUrl = registration.sqsQueueUrl,
            eventHandler = registration,
            client = sqsConfiguration.amazonSQSClient(),
            tabourConfigs = tabourAutoConfigs,
            pollerConfigs = pollerConfigs
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        for (poller in pollers)
            poller.start()
    }

    fun stop() {
        for (poller in pollers) {
            poller.stop()
        }
    }
}
