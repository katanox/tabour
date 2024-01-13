package com.katanox.tabour.proto.mapper

import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.util.JsonFormat
import software.amazon.awssdk.services.sqs.model.Message

private val printer: JsonFormat.Printer = JsonFormat.printer()
val parser: JsonFormat.Parser = JsonFormat.parser()

/** Converts a SQS message to a Protobuf message using the builder of the Protobuf message */
inline fun <T : ProtobufMessage.Builder, reified K> T.fromSqsMessage(message: Message): K {
    parser.merge(message.body(), this)
    return this.build() as K
}

@JvmName("fromSqsMessageOrNullWithError")
inline fun <T : ProtobufMessage.Builder, reified K> T.fromSqsMessageOrNull(
    message: Message,
    onError: (Throwable) -> Unit
): K? =
    try {
        fromSqsMessage<T, K>(message)
    } catch (e: Throwable) {
        onError(e)
        null
    }

inline fun <T : ProtobufMessage.Builder, reified K, A> T.fromSqsMessageOrNull(
    message: Message,
    onError: (Throwable) -> Unit,
    map: (K) -> A
): A? =
    try {
        map(fromSqsMessage<T, K>(message))
    } catch (e: Throwable) {
        onError(e)
        null
    }

inline fun <T : ProtobufMessage.Builder, reified K> T.fromSqsMessageOrNull(message: Message): K? =
    try {
        fromSqsMessage<T, K>(message)
    } catch (e: Throwable) {
        null
    }

@JvmName("fromSqsMessageOrNullWithMap")
inline fun <T : ProtobufMessage.Builder, reified K, A> T.fromSqsMessageOrNull(
    message: Message,
    map: (K) -> A
): A? =
    try {
        map(fromSqsMessage<T, K>(message))
    } catch (e: Throwable) {
        null
    }

/** Converts a protobuf message to a json which can also be parsed by a protobuf message builder */
fun ProtobufMessage.jsonify(): String = printer.print(this)
