package com.katanox.tabour.plug

interface ConsumerPlug {
    suspend fun <T> succ(data: T)
    suspend fun <T, E> fail(data: T, error: E)
}

interface ProducerPlug {
    suspend fun <T> succ(data: T)
    suspend fun <T, E> fail(data: T, error: E)
}
