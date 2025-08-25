package com.katanox.tabour.proto.mapper

import aws.sdk.kotlin.services.sqs.model.Message
import com.google.protobuf.Message as ProtobufMessage
import com.google.protobuf.util.JsonFormat
import kotlin.jvm.Throws

private val printer: JsonFormat.Printer = JsonFormat.printer()
val parser: JsonFormat.Parser = JsonFormat.parser()

/**
 * Deserializes a [message] to a protobuf object. The function can throw an exception if the
 * deserialization fails
 */
@Throws(Throwable::class)
inline fun <T : ProtobufMessage.Builder, reified K> T.fromSqsMessage(message: Message): K {
    parser.merge(message.body, this)
    return this.build() as K
}

@JvmName("fromSqsMessageOrNullWithError")
/**
 * Deserializes a [message] to a protobuf object. The function returns a null if the deserialization
 * fails and invokes [onError]
 */
inline fun <T : ProtobufMessage.Builder, reified K> T.fromSqsMessageOrNull(
    message: Message,
    onError: (Throwable) -> Unit,
): K? =
    try {
        fromSqsMessage<T, K>(message)
    } catch (e: Throwable) {
        onError(e)
        null
    }

/**
 * Deserializes a [message] to a protobuf object. The function returns a null if the deserialization
 * fails and invokes [onError]. If the message is successfully deserialized, [map] is invoked with
 * the deserialized value
 */
inline fun <T : ProtobufMessage.Builder, reified K, A> T.fromSqsMessageOrNull(
    message: Message,
    onError: (Throwable) -> Unit,
    map: (K) -> A,
): A? =
    try {
        map(fromSqsMessage<T, K>(message))
    } catch (e: Throwable) {
        onError(e)
        null
    }

/*
 * Deserializes a [message] to a protobuf object. The function returns a null if the deserialization
 * fails.
 */
inline fun <T : ProtobufMessage.Builder, reified K> T.fromSqsMessageOrNull(message: Message): K? =
    try {
        fromSqsMessage<T, K>(message)
    } catch (e: Throwable) {
        null
    }

@JvmName("fromSqsMessageOrNullWithMap")
/**
 * Deserializes a [message] to a protobuf object and invokes [map] with the deserialized value. The
 * function can throw an exception during deserialization
 */
@Throws(Throwable::class)
inline fun <T : ProtobufMessage.Builder, reified K, A> T.fromSqsMessageOrNull(
    message: Message,
    map: (K) -> A,
): A? =
    try {
        map(fromSqsMessage<T, K>(message))
    } catch (e: Throwable) {
        null
    }

/** Converts a protobuf message to a json which can also be parsed by a protobuf message builder */
fun ProtobufMessage.jsonify(): String = printer.print(this)
