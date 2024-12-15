package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import java.net.URL
import software.amazon.awssdk.services.sqs.model.Message

class SqsConsumer<T>
internal constructor(
    val queueUri: URL,
    val key: T,
    override val onSuccess: suspend (Message) -> Boolean,
    override val onError: suspend (ConsumptionError) -> Unit,
) : Consumer<Message, ConsumptionError>, Config {
    var config: SqsConsumerConfiguration = sqsConsumerConfiguration { maxMessages = 10 }
}
