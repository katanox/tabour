package config

import software.amazon.awssdk.services.sqs.model.Message
import java.time.Duration
import java.time.temporal.ChronoUnit

enum class IntegrationType {
    SQS
}

sealed interface Consumer {
    fun <T> onSuccess(successResult: T)
    fun onFail(exception: Exception)

    val type: IntegrationType
}

class SqsConfiguration : Consumer {
    override val type = IntegrationType.SQS

    var url: String = ""
    var maxMessages = 10
    val waitTime = 10
    val workers = 1
    val sleepTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)
    var successFn: (List<Message>) -> Unit = {}
    var errorFn: (Exception) -> Unit = {}

    @Suppress("UNCHECKED_CAST")
    override fun <T> onSuccess(successResult: T): Unit = successFn(successResult as List<Message>)

    override fun onFail(exception: Exception): Unit = errorFn(exception)
}
