package com.katanox.tabour.plug

interface ConsumerPlug {
    suspend fun <T> onSuccess(data: T)
    suspend fun <T, E> onFailure(data: T, error: E)
}

interface ProducerPlug {
    suspend fun <T> onSuccess(data: T)
    suspend fun <T, E> onFailure(data: T, error: E)
}
