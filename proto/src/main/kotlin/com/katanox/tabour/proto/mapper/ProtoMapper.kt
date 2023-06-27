package com.katanox.tabour.proto.mapper

import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.util.JsonFormat
import software.amazon.awssdk.services.sqs.model.Message

private val printer = JsonFormat.printer()

inline fun <reified T : ProtobufMessage> ProtobufMessage.Builder.fromSqsMessage(
    message: Message
): T {
    val parser = JsonFormat.parser()
    parser.merge(message.body(), this)
    return this.build() as T
}

fun ProtobufMessage.jsonify(): String = printer.print(this)
