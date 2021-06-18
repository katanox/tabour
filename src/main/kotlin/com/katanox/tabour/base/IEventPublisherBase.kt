package com.katanox.tabour.base

import com.katanox.tabour.factory.BusType
import java.io.Serializable

interface IEventPublisherBase {
    fun getType(): BusType
    fun <T: Serializable> publish(message: T, busUrl: String)
}