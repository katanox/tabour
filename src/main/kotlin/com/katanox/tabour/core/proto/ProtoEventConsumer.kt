package com.katanox.tabour.core.proto

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import com.katanox.tabour.core.EventConsumer

abstract class ProtoEventConsumer<T: Message.Builder>: EventConsumer() {
    fun parseMessageToEvent(message: String, event: T): T {
        JsonFormat.parser().merge(message, event)
        return event
    }
}
