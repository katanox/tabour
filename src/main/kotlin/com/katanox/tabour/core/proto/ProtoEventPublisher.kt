package com.katanox.tabour.core.proto

import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.katanox.tabour.core.EventPublisher

abstract class ProtoEventPublisher : EventPublisher() {
    fun publish(event: MessageOrBuilder, busUrl: String, messageGroupId: String? = null) {
        val json: String = JsonFormat.printer().includingDefaultValueFields().print(event)
        this.publish(json, busUrl, messageGroupId)
    }
}
