package com.katanox.tabour.base

import com.amazonaws.services.sqs.model.Message
import com.katanox.tabour.factory.BusType

interface IEventPublisherBase {
    fun getType(): BusType
    fun publish(message: String, busUrl: String, messageGroupId: String?)
    fun delete(receiptHandle: String, busUrl: String): Boolean
}
