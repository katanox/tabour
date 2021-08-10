package com.katanox.tabour.base

import com.amazonaws.services.sqs.model.Message
import com.katanox.tabour.factory.BusType

typealias DeletableMessageCovnerter = (msg: Any) -> DeletableMessage

data class DeletableMessage(val id: String)

/**
 * functions to convert different type of messages to a deletable message which can be used
 * by the publisher's delete method.
 */
val sqsMessageToDeletableMessageConverter: DeletableMessageCovnerter = { m -> DeletableMessage((m as Message).receiptHandle) }

fun getDeletableMessageProducer(busType: BusType): DeletableMessageCovnerter {
    return when(busType) {
       BusType.SQS -> sqsMessageToDeletableMessageConverter
    }
}