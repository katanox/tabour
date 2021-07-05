package com.katanox.tabour.core

import com.katanox.tabour.base.IEventConsumerBase
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.factory.EventConsumerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class EventConsumer {

    private lateinit var consumerBase: IEventConsumerBase

    @Autowired private lateinit var eventConsumerFactory: EventConsumerFactory

    abstract fun getBusURL(): String

    abstract fun getBusType(): BusType

    abstract fun consume(message: String)

    @PostConstruct
    private fun setUp() {
        consumerBase =
            eventConsumerFactory.getEventConsumer(getBusType(), getBusURL()) { consume(it as String) }
    }
}
