package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.services.sqs.model.BatchResultErrorEntry
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.ServiceException
import java.net.URL

sealed interface ProductionError {
    /** The producer url is empty */
    data class EmptyUrl(val url: URL) : ProductionError

    /** The production of a batch of messages failed */
    data class FailedBatch(val errors: List<BatchResultErrorEntry>) : ProductionError

    /** The response does not contain a message id after the message was sent to the queue */
    data object EmptyMessageId : ProductionError

    data class UnrecognizedError(val error: Throwable) : ProductionError

    data class AwsServiceError(val exception: ServiceException) : ProductionError

    data class AwsClientError(val exception: ClientException) : ProductionError
}
