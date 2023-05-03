package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.configuration.sqs.sqsPipeline
import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.production.SqsProducer
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import software.amazon.awssdk.services.sqs.model.Message


class SqsConsumer internal constructor() : Consumer<Message, ConsumptionError>, Config {
    private var onSuccessWasSet: Boolean = false
    private var pipelineWasSet: Boolean = false

    internal var handlerWasSet = onSuccessWasSet.xor(pipelineWasSet)

    override var onSuccess: (Message) -> Unit = {}
        set(value) {
            if (pipelineWasSet) {
                throw IllegalArgumentException("Can not set `onSuccess` if pipeline is set")
            }
            onSuccessWasSet = true
            field = value
        }

    override var onError: (ConsumptionError) -> Unit = {}

    var pipeline: SqsPipeline = sqsPipeline {}
        set(value) {
            if (pipelineWasSet) {
                throw IllegalArgumentException("Can not set `pipeline `if `onSuccess `is set")
            }
            pipelineWasSet = true
            field = value
        }

    var queueUrl: URI = URI("")

    var config: SqsConsumerConfiguration = sqsConsumerConfiguration { maxMessages = 1 }
}

