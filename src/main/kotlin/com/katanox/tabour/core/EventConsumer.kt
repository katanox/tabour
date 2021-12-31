package com.katanox.tabour.core

import com.katanox.tabour.base.IEventHandlerBase
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.factory.EventHandlerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class EventConsumer {

    private lateinit var handler: IEventHandlerBase

    @Autowired private lateinit var eventHandlerFactory: EventHandlerFactory

    abstract fun getBusURL(): String

    abstract fun getBusType(): BusType

    abstract fun consume(message: String)

    @PostConstruct
    private fun setUp() {
        handler =
            eventHandlerFactory.getEventHandler(getBusType(), getBusURL()) { consume(it as String) }
    }
}
