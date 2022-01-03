package com.katanox.tabour.core.proto

import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.katanox.tabour.core.EventPublisher

abstract class ProtoEventPublisher : EventPublisher() {
    fun publish(event: MessageOrBuilder, busUrl: String, messageGroupId: String? = null) {
        val json: String = JsonFormat.printer().print(event)
        this.publish(json, busUrl, messageGroupId)
    }

    fun publishEventBatch(events: List<MessageOrBuilder>, busUrl: String, messageGroupId: String? = null) {
        val parsedEvents = events.map { JsonFormat.printer().print(it) }
        this.publishBatch(parsedEvents, busUrl, messageGroupId)
    }
}
