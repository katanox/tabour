package com.katanox.tabour.consumption

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.services.sqs.model.Message

sealed interface ConsumptionError {
    data class AwsServiceError(val exception: AwsServiceException) : ConsumptionError

    data class AwsClientError(val exception: ClientException) : ConsumptionError

    /**
     * Represents an error is returned when the [Consumer.onSuccess] method does not return true
     * which has a result of the message not being acknowledged
     */
    data class UnsuccessfulConsumption(val message: Message) : ConsumptionError

    /** Represents an exception that was thrown while invoking [Consumer.onSuccess] */
    data class ThrowableDuringHanding(val throwable: Throwable) : ConsumptionError

    /**
     * The error is returned for all the errors other than [AwsServiceError], [AwsClientError],
     * [UnsuccessfulConsumption] and [ThrowableDuringHanding]
     */
    data class UnrecognizedError(val error: Throwable) : ConsumptionError
}
