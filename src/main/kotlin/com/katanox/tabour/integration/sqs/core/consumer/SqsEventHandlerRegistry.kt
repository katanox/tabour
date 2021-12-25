package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.exception.ExceptionHandler
import com.katanox.tabour.integration.sqs.config.SqsConfiguration
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SqsEventHandlerRegistry(
    eventHandlers: List<SqsEventHandler>,
    var eventPollerProperties: EventPollerProperties,
    private var sqsConfiguration: SqsConfiguration,
    private val sqsEventFetcher: SqsEventFetcher,
) {
    private var pollers: Set<SqsEventPoller> = setOf()

    init {
        pollers = initializePollers(eventHandlers)
    }

    private fun initializePollers(registrations: List<SqsEventHandler>): Set<SqsEventPoller> {
        val pollers: MutableSet<SqsEventPoller> = HashSet()
        for (registration in registrations) {
            pollers.add(createPollerForHandler(registration))
            logger.info("initialized SqsMessagePoller '{}'", registration.javaClass.canonicalName)
        }
        return pollers
    }

    private fun createPollerForHandler(registration: SqsEventHandler): SqsEventPoller {
        return SqsEventPoller(
            queueUrl = registration.sqsQueueUrl,
            eventHandler = registration,
            eventFetcher = sqsEventFetcher,
            pollingProperties = eventPollerProperties,
            client = sqsConfiguration.amazonSQSAsync(),
            exceptionHandler = ExceptionHandler.defaultExceptionHandler()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        for (poller in pollers) {
            runBlocking {
                withContext(newCoroutineContext(Dispatchers.IO)) {
                    poller.start()
                }
            }
        }
    }

    fun stop() {
        for (poller in pollers) {
            poller.stop()
        }
    }
}
