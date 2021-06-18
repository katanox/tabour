package com.katanox.tabour.core

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.factory.EventPublisherFactory
import com.katanox.tabour.factory.BusType
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import javax.annotation.PostConstruct

abstract class EventPublisher<T : Serializable> {

    private lateinit var eventPublisher: IEventPublisherBase

    @Autowired
    private lateinit var eventPublisherFactory: EventPublisherFactory

    abstract fun getBusType(): BusType

    @PostConstruct
    private fun setUp() {
        eventPublisher = eventPublisherFactory.getEventPublisher(getBusType())
    }

    open fun publish(message: T, busUrl: String) {
        eventPublisher.publish(message, busUrl)
    }
}