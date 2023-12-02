package com.katanox.tabour.sqs.production

import java.net.URL
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.exception.SdkClientException

sealed interface ProductionError {
    data class EmptyUrl(val url: URL) : ProductionError

    data class EmptyMessage<T>(val message: T) : ProductionError

    data class UnrecognizedError(val error: Throwable) : ProductionError

    data class AwsError(val details: AwsErrorDetails) : ProductionError

    data class AwsSdkClientError(val exception: SdkClientException) : ProductionError
}
