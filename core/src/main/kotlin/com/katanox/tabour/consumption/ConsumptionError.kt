package com.katanox.tabour.consumption

import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.model.Message

sealed interface ConsumptionError {
    /** Represents an error which AWS returns during the request of retrieving messages */
    data class AwsError(val details: AwsErrorDetails) : ConsumptionError

    /** An error which is raised from the SQS SDK */
    data class AwsSdkClientError(val exception: SdkClientException) : ConsumptionError

    /**
     * Represents an error is returned when the [Consumer.onSuccess] method does not return true
     * which has a result of the message not being acknowledged
     */
    data class UnsuccessfulConsumption(val message: Message) : ConsumptionError

    /** Represents an exception that was thrown while invoking [Consumer.onSuccess] */
    data class ThrowableDuringHanding(val throwable: Throwable) : ConsumptionError

    /**
     * The error is returned for all the errors other than [AwsError], [AwsSdkClientError],
     * [UnsuccessfulConsumption] and [ThrowableDuringHanding]
     */
    data class UnrecognizedError(val error: Throwable) : ConsumptionError
}
