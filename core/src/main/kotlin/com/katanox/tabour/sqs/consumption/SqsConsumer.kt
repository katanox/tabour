package com.katanox.tabour.sqs.consumption

import aws.sdk.kotlin.services.sqs.model.Message
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.consumption.Consumer
import com.katanox.tabour.consumption.ConsumptionError
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Used to create consumers that will be registered to a [com.katanox.tabour.sqs.SqsRegistry] in
 * order to consume messages. An instance of it can be created using
 * [com.katanox.tabour.configuration.sqs.sqsConsumer]
 */
class SqsConsumer<T>
internal constructor(
    /** The queue url to consume */
    val queueUri: URL,
    /** The key of the consumer which is used to identify unique consumers */
    val key: T,
    /**
     * The handler which is called when we retrieve a message from SQS. The result of the function
     * indicates if the consumption was successful or not, in order to acknowledge the message
     */
    override val onSuccess: suspend (Message) -> Boolean,
    /** The handler that is called in any failure while consuming messages */
    override val onError: suspend (ConsumptionError) -> Unit,
) : Consumer<Message, ConsumptionError>, Config {
    var config: Configuration = sqsConsumerConfiguration {}

    class Configuration internal constructor() : Config {

        /**
         * A function that is used to configure the receive request of the consumer before before
         * the request is sent to sqs
         */
        var receiveRequestConfigurationBuilder: (ReceiveMessageRequest.Builder.() -> Unit) = {
            maxNumberOfMessages = 10
        }

        /**
         * The number of parallel workers that will be spawned to fetch messages from the queue
         *
         * @throws IllegalArgumentException when the value is not between the bound 0 and 50
         */
        var concurrency: Int = 1
            @Throws(IllegalArgumentException::class)
            set(value) {
                require(value in 0..50)
                field = value
            }

        /**
         * The delay between subsequent requests
         *
         * Default is 10 seconds
         */
        var sleepTime: Duration = 10.seconds
    }
}
