package com.katanox.tabour.core

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.factory.EventPublisherFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class EventPublisher {

    private lateinit var publisherBase: IEventPublisherBase

    @Autowired
    private lateinit var eventPublisherFactory: EventPublisherFactory

    abstract fun getBusType(): BusType

    @PostConstruct
    private fun setUp() {
        publisherBase = eventPublisherFactory.getEventPublisher(getBusType())
    }

    open fun publish(message: String, busUrl: String, messageGroupId: String? = null) {
        publisherBase.publish(message, busUrl, messageGroupId)
    }

    open fun publishBatch(messages: List<String>, busUrl: String, messagesGroupId: String? = null) {
        publisherBase.publishBatch(messages, busUrl, messagesGroupId)
    }
}
