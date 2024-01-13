package com.katanox.tabour.plug

sealed class PlugRecord<T> {
    abstract val key: T
}

data class SuccessPlugRecord<T, K>(val data: T, override val key: K) : PlugRecord<K>()

data class FailurePlugRecord<T, K, E>(val data: T, override val key: K, val error: E) :
    PlugRecord<K>()

interface ConsumerPlug {
    suspend fun <T> handle(record: PlugRecord<T>)
}

interface ProducerPlug {
    suspend fun <T> handle(record: PlugRecord<T>)
}
