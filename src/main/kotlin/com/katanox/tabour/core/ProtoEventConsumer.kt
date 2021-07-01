package com.katanox.tabour.core

import com.google.protobuf.Message
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat

abstract class ProtoEventConsumer<T: Message.Builder>: EventConsumer() {
    fun consume(message: String, event: T): T {
        JsonFormat.parser().merge(message, event)
        return event
    }
}