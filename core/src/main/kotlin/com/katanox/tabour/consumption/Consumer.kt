package com.katanox.tabour.consumption

internal interface Consumer<T, K : ConsumptionError> {
    var onSuccess: (T) -> Unit
    var onError: (K) -> Unit
}
