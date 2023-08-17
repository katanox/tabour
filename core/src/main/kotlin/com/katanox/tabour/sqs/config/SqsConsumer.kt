package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import java.net.URL
import software.amazon.awssdk.services.sqs.model.Message

class SqsConsumer internal constructor(val queueUri: URL) :
    Consumer<Message, ConsumptionError>, Config {

  override var onSuccess: suspend (Message) -> Boolean = { false }

  override var onError: (ConsumptionError) -> Unit = {}

  var pipeline: SqsPipeline? = null

  var config: SqsConsumerConfiguration = sqsConsumerConfiguration { maxMessages = 1 }
}
