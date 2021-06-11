package com.katanox.tabour.factory

import com.katanox.tabour.base.IEventPublisherBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class EventPublisherFactory @Autowired constructor(services: List<IEventPublisherBase>) {

    private val busTypeEventPublisherHashMap: MutableMap<BusType, IEventPublisherBase> =
        EnumMap(BusType::class.java)

    init {
        for (service in services) {
            busTypeEventPublisherHashMap[service.getType()] = service
        }
    }

    fun getEventPublisher(type: BusType): IEventPublisherBase {
        return busTypeEventPublisherHashMap[type]
            ?: throw RuntimeException("Unknown service type: $type")
    }
}