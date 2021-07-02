package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventHandlerProperties
import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.exception.ExceptionHandler
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import com.katanox.tabour.thread.ThreadPools
import mu.KotlinLogging
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor

private val logger = KotlinLogging.logger {}


class SqsEventHandlerRegistry(
    eventHandlers: List<SqsEventHandler>,
    var eventHandlerProperties: EventHandlerProperties,
    var eventPollerProperties: EventPollerProperties,
    var sqsConfiguration: SqsConfiguration
) {
    private var pollers: Set<SqsEventPoller> = setOf()

    init {
        pollers = initializePollers(eventHandlers)
    }

    private fun initializePollers(
        registrations: List<SqsEventHandler>
    ): Set<SqsEventPoller> {
        val pollers: MutableSet<SqsEventPoller> = HashSet()
        for (registration in registrations) {
            pollers.add(createPollerForHandler(registration))
            logger.info("initialized SqsMessagePoller '{}'", registration.javaClass.canonicalName)
        }
        return pollers
    }

    private fun createPollerForHandler(
        registration: SqsEventHandler
    ): SqsEventPoller {
        return SqsEventPoller(
            name = registration.javaClass.canonicalName,
            eventHandler = registration,
            eventFetcher = createFetcherForHandler(registration),
            pollerThreadPool = createPollingThreadPool(registration),
            handlerThreadPool = createHandlerThreadPool(registration),
            pollingProperties = eventPollerProperties,
            sqsConfiguration = sqsConfiguration,
            exceptionHandler = ExceptionHandler.defaultExceptionHandler()
        )
    }

    private fun createFetcherForHandler(registration: SqsEventHandler): SqsEventFetcher {
        return SqsEventFetcher(registration.sqsQueueUrl, sqsConfiguration, eventPollerProperties)
    }

    private fun createPollingThreadPool(
        registration: SqsEventHandler
    ): ScheduledThreadPoolExecutor {
        return ThreadPools.blockingScheduledThreadPool(
            eventPollerProperties.pollingThreads,
            String.format("%s-poller", registration.javaClass.canonicalName)
        )
    }

    private fun createHandlerThreadPool(
        registration: SqsEventHandler
    ): ThreadPoolExecutor {
        return ThreadPools.blockingThreadPool(
            eventHandlerProperties.threadPoolSize,
            eventHandlerProperties.queueSize,
            String.format("%s-handler", registration.javaClass.canonicalName)
        )
    }

    fun start() {
        for (poller in pollers) {
            poller.start()
        }
    }

    fun stop() {
        for (poller in pollers) {
            poller.stop()
        }
    }

}