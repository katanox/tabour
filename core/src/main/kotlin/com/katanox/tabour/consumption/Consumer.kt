package com.katanox.tabour.consumption

import com.katanox.tabour.plug.ConsumerPlug

internal interface Consumer<T, K : ConsumptionError> {
    /**
     *
     */
    var onSuccess: suspend (T) -> Boolean
    var onError: (K) -> Unit
    val plugs: MutableList<ConsumerPlug>
}
