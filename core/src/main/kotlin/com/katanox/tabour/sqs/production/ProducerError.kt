package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.services.sqs.model.BatchResultErrorEntry
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.ServiceException
import java.net.URL

sealed interface ProductionError {
    data class EmptyUrl(val url: URL) : ProductionError

    data class FailedBatch(val errors: List<BatchResultErrorEntry>) : ProductionError

    data object EmptyMessageId : ProductionError

    data class UnrecognizedError(val error: Throwable) : ProductionError

    data class AwsServiceError(val exception: ServiceException) : ProductionError

    data class AwsClientError(val exception: ClientException) : ProductionError
}
