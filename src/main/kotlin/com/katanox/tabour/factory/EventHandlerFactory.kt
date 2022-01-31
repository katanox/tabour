package com.katanox.tabour.factory

import com.katanox.tabour.base.IEventHandlerBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.extentions.ConsumerAction
import com.katanox.tabour.extentions.FailureAction
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandler
import java.util.EnumMap

class EventHandlerFactory(private val tabourAutoConfigs: TabourAutoConfigs) {

    private val eventConsumers = EnumMap<BusType, List<IEventHandlerBase>>(BusType::class.java)

    init {
        BusType.values().forEach { eventConsumers[it] = ArrayList() }
    }

    fun getEventHandler(
        type: BusType,
        busName: String,
        consume: ConsumerAction,
        failureAction: FailureAction,
    ): IEventHandlerBase {
        return when (type) {
            BusType.SQS -> {
                val handler = SqsEventHandler(busName, consume, tabourAutoConfigs, failureAction)
                (eventConsumers[BusType.SQS] as ArrayList).add(handler)
                handler
            }
        }
    }

    fun getEventHandlers(type: BusType): List<IEventHandlerBase> {
        return when (type) {
            BusType.SQS -> {
                eventConsumers[type] ?: listOf()
            }
        }
    }
}
