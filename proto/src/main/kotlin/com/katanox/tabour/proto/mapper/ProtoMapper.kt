package com.katanox.tabour.proto.mapper

import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.util.JsonFormat
import software.amazon.awssdk.services.sqs.model.Message

private val printer = JsonFormat.printer()

/** Converts a SQS message to a Protobuf message using the builder of the Protobuf message */
inline fun <reified T : ProtobufMessage> ProtobufMessage.Builder.fromSqsMessage(
    message: Message
): T {
    val parser = JsonFormat.parser()
    parser.merge(message.body(), this)
    return this.build() as T
}

/** Converts a protobuf message to a json which can also be parsed by a protobuf message builder */
fun ProtobufMessage.jsonify(): String = printer.print(this)
