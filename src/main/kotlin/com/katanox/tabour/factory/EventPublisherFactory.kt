package com.katanox.tabour.factory

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.exception.PublisherNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.EnumMap

@Component
class EventPublisherFactory @Autowired constructor(services: List<IEventPublisherBase>) {

    private val eventPublishers: MutableMap<BusType, IEventPublisherBase> =
        EnumMap(BusType::class.java)

    init {
        for (service in services) {
            eventPublishers[service.getType()] = service
        }
    }

    fun getEventPublisher(type: BusType): IEventPublisherBase {
        return eventPublishers[type] ?: throw PublisherNotFoundException("Unknown service type: $type")
    }
}
