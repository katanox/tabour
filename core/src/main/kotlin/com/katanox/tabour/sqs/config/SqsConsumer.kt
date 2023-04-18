package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.*
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.production.SqsProducer
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import software.amazon.awssdk.services.sqs.model.Message

class SqsConsumer internal constructor() : Consumer<Message, ConsumptionError>, Config {
    override var onSuccess: (Message) -> Unit = {}
    override var onError: (ConsumptionError) -> Unit = {}

    val producer: SqsProducer = sqsProducer {
        queueUrl = URI("")
        key = "heheh"
        produce = { "asd" }
    }

    var queueUrl: URI = URI("")

    var config: SqsConsumerConfiguration = sqsConsumerConfiguration { maxMessages = 10 }
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

    var retries: Int = 1
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Negative values are not allowed")
            }
            field = value
        }
}
