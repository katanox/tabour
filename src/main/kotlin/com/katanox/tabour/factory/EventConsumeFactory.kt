package com.katanox.tabour.factory

import com.katanox.tabour.base.IEventConsumerBase
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandler
import org.springframework.stereotype.Component

@Component
class EventConsumeFactory{

    fun getEventConsumer(type: BusType): IEventConsumerBase {
        return when(type){
            BusType.SQS -> SqsEventHandler()
        }
    }
}