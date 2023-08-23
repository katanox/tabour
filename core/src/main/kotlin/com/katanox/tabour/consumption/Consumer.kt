package com.katanox.tabour.consumption

internal interface Consumer<T, K : ConsumptionError> {
    var onSuccess: suspend (T) -> Boolean
    var onError: (K) -> Unit
}
