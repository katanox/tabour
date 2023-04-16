package com.katanox.tabour.sqs.config

import com.katanox.tabour.*
import com.katanox.tabour.Config
import com.katanox.tabour.Consumer
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import software.amazon.awssdk.services.sqs.model.Message


class SqsQueueConfiguration internal constructor() :
    Consumer<Message, ConsumptionError.AwsError>, Config {
    override val type = IntegrationType.SQS

    override var onSuccess: (Message) -> Unit = {}
    override var onError: (ConsumptionError.AwsError) -> Unit = {}

    var queueUrl: URI = URI("")

    var config: SqsConsumerConfiguration =
        sqsConsumerConfiguration { maxMessages = 10 }
}

class SqsConsumerConfiguration internal constructor() : Config {
    /** The number of max messages to fetch. Default is 1 with max being 10 */
    var maxMessages: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }

    /**
     * Unit of concurrency. In combination with [maxMessages] determines the max number of messages
     * that can be received every [sleepTime]. The upper bound of messages every [sleepTime] is
     * [maxMessages] * [concurrency]
     */
    var concurrency: Int = 1
        set(value) {
            if (value > 10 || value < 0) {
                throw IllegalArgumentException("Value must be 0-10")
            }
            field = value
        }

    var sleepTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)
}
