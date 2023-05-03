package com.katanox.tabour.proto.mapper

import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.util.JsonFormat
import software.amazon.awssdk.services.sqs.model.Message

private val parser = JsonFormat.parser()
private val printer = JsonFormat.printer()

fun ProtobufMessage.Builder.fromSqsMessage(message: Message): ProtobufMessage {
    parser.merge(message.body(), this)
    return this.build()
}

fun ProtobufMessage.jsonify(): String = printer.print(this)
