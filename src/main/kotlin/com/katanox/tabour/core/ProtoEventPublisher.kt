package com.katanox.tabour.core

import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import kotlin.reflect.jvm.internal.impl.protobuf.MessageLiteOrBuilder

abstract class ProtoEventPublisher: EventPublisher() {
    fun publish(event: MessageOrBuilder, busUrl: String) {
        val json: String = JsonFormat.printer().print(event)
        this.publish(json, busUrl)
    }
}