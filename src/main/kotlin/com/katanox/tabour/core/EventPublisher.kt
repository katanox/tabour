package com.katanox.tabour.core

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.factory.EventPublisherFactory
import com.katanox.tabour.factory.BusType
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class EventPublisher {

    private lateinit var eventPublisher: IEventPublisherBase

    @Autowired
    private lateinit var eventPublisherFactory: EventPublisherFactory

    abstract fun getBusType(): BusType

    @PostConstruct
    private fun setUp() {
        eventPublisher = eventPublisherFactory.getEventPublisher(getBusType())
    }

    open fun publish(message: String, busUrl: String, messageGroupId: String? = null) {
        eventPublisher.publish(message, busUrl,messageGroupId)
    }
}