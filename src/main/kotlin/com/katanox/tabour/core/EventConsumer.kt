package com.katanox.tabour.core

import com.katanox.tabour.base.IEventHandlerBase
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.factory.EventHandlerFactory
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

abstract class EventConsumer {

    private lateinit var handler: IEventHandlerBase

    @Autowired
    private lateinit var eventHandlerFactory: EventHandlerFactory

    abstract fun getBusURL(): String

    abstract fun getBusType(): BusType

    abstract fun consume(message: String)

    /**
     * The action that will be executed in case of failure to consume a message.
     * default behavior is to log the exception.
     *
     * Possible to be overridden by the consumer to execute different action
     */
    open fun onFailure(throwable: Throwable, message: Any) {
        logger.warn { "error ${throwable.message} happened while processing the message $message" }
    }

    @PostConstruct
    private fun setUp() {
        handler = eventHandlerFactory.getEventHandler(
            getBusType(),
            getBusURL(),
            { consume(it) },
            { throwable, message -> onFailure(throwable, message) }
        )
    }
}
