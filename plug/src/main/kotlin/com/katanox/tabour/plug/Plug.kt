package com.katanox.tabour.plug

sealed class PlugRecord<T> {
    abstract val key: T
}

data class SuccessPlugRecord<T, K>(val data: T, override val key: K) : PlugRecord<K>()

data class FailurePlugRecord<T, K, E>(val data: T, override val key: K, val error: E) :
    PlugRecord<K>()

/**
 * This interface is supposed to be implemented by classes that want to be triggered every time a
 * message is consumed.
 *
 * If the message is successfully consumed, then [handle] will be invoked with a
 * [SuccessPlugRecord]. Otherwise it will be invoked with [FailurePlugRecord]
 */
interface ConsumerPlug {
    suspend fun <T> handle(record: PlugRecord<T>)
}

/**
 * This interface is supposed to be implemented by classes that want to be triggered every time a
 * message is produced.
 *
 * If the message is successfully consumed, then [handle] will be invoked with a
 * [SuccessPlugRecord]. Otherwise it will be invoked with [FailurePlugRecord]
 */
interface ProducerPlug {
    suspend fun <T> handle(record: PlugRecord<T>)
}
