package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.plug.ConsumerPlug
import java.net.URL
import software.amazon.awssdk.services.sqs.model.Message

class SqsConsumer<T> internal constructor(val queueUri: URL, val key: T) :
    Consumer<Message, ConsumptionError>, Config {

    override var onSuccess: suspend (Message) -> Boolean = { false }

    override var onError: (ConsumptionError) -> Unit = {}

    override var plugs: MutableList<ConsumerPlug> = mutableListOf()

    var config: SqsConsumerConfiguration = sqsConsumerConfiguration { maxMessages = 10 }
}
