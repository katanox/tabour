package com.katanox.tabour.base

import com.katanox.tabour.factory.BusType

interface IEventPublisherBase {
    fun getType(): BusType
    fun publish(message: String, busUrl: String, messageGroupId: String?)
    fun publishBatch(message: List<String>, busUrl: String, messageGroupId: String?)
}
