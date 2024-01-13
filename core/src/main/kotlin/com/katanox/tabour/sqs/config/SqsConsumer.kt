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

    /**
     * This is the handler for each message retrieved from SQS
     *
     * Return value indicates if the message was successfully processed. If the function returns
     * false, then [onError] is invoked with [ConsumptionError.UnsuccessfulConsumption] error
     *
     * If an exception is raised during the invocation of the function, [onError] is automatically
     * invoked
     */
    override var onSuccess: suspend (Message) -> Boolean = { false }

    /**
     * Error handler which is invoked whenever the message is not successfully processed. The method
     * is also invoked if an exception is raised in [onSuccess]
     *
     * If onError is invoked, the message is not acknowledged.
     */
    override var onError: (ConsumptionError) -> Unit = {}

    override val plugs: MutableList<ConsumerPlug> = mutableListOf()

    var config: SqsConsumerConfiguration = sqsConsumerConfiguration { maxMessages = 10 }
}
