package com.katanox.tabour.base

import com.amazonaws.services.sqs.model.Message
import com.katanox.tabour.factory.BusType

typealias DeletableMessageConverter = (message: Any) -> DeletableMessage

data class DeletableMessage(val id: String)

/**
 * functions to convert different type of messages to a deletable message which can be used
 * by the publisher's delete method.
 */
val sqsMessageToDeletableMessageConverter: DeletableMessageConverter = { m -> DeletableMessage((m as Message).receiptHandle) }

fun getDeletableMessageProducer(message: Any, busType: BusType): DeletableMessage {
    return when (busType) {
        BusType.SQS -> sqsMessageToDeletableMessageConverter(message)
    }
}
