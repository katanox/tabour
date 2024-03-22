package com.katanox.tabour.consumption

import com.katanox.tabour.plug.ConsumerPlug

internal interface Consumer<T, K : ConsumptionError> {
    /**
     * This is the handler for each message retrieved from SQS
     *
     * Return value indicates if the message was successfully processed. If the function returns
     * false, then [onError] is invoked with [ConsumptionError.UnsuccessfulConsumption] error
     *
     * If an exception is raised during the invocation of the function, [onError] is automatically
     * invoked
     */
    val onSuccess: suspend (T) -> Boolean

    /**
     * Error handler which is invoked whenever the message is not successfully processed. The method
     * is also invoked if an exception is raised in [onSuccess]
     *
     * If onError is invoked, the message is not acknowledged.
     */
    val onError: suspend (K) -> Unit

    val plugs: MutableList<ConsumerPlug>
}
