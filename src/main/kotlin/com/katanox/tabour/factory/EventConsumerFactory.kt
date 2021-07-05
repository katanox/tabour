package com.katanox.tabour.factory

import com.katanox.tabour.base.IEventConsumerBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandler
import java.util.EnumMap

class EventConsumerFactory(private val tabourAutoConfigs: TabourAutoConfigs) {

    private val eventConsumers = EnumMap<BusType, List<IEventConsumerBase>>(BusType::class.java)

    init {
        BusType.values().forEach { eventConsumers[it] = ArrayList() }
    }

    fun getEventConsumer(type: BusType, busName: String, consume: (Any) -> Unit): IEventConsumerBase {
        return when (type) {
            BusType.SQS -> {
                val handler = SqsEventHandler(busName, consume, tabourAutoConfigs)
                (eventConsumers[BusType.SQS] as ArrayList).add(handler)
                handler
            }
        }
    }

    fun getEventConsumers(type: BusType): List<IEventConsumerBase> {
        return when (type) {
            BusType.SQS -> {
                eventConsumers[type] ?: listOf()
            }
        }
    }
}
