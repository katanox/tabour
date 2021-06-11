package com.katanox.tabour.core

import com.katanox.tabour.base.IEventConsumerBase
import com.katanox.tabour.factory.EventConsumeFactory
import com.katanox.tabour.factory.BusType
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class EventConsumer {

    private lateinit var eventConsumer: IEventConsumerBase

    @Autowired
    private lateinit var eventConsumerFactory: EventConsumeFactory

    abstract fun getBusName(): String

    abstract fun getBusType(): BusType

    abstract fun consume(message: ByteArray)

    @PostConstruct
    private fun setUp() {
        eventConsumer = eventConsumerFactory.getEventConsumer(getBusType())
        eventConsumer.setBusUrl(getBusName())
        eventConsumer.setAction { consume(it) }
    }

}