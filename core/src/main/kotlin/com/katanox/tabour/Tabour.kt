package com.katanox.tabour

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.SqsRegistry
import kotlinx.coroutines.*

class Tabour internal constructor(configuration: Configuration) {
    private val registries: MutableSet<Registry<*>> = mutableSetOf()

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher =
        newFixedThreadPoolContext(configuration.numOfThreads, "tabour")
    private var consumptionStarted: Boolean = false
    private val scope = CoroutineScope(dispatcher)

    fun <T> register(registry: Registry<T>): Tabour = this.apply { registries.add(registry) }

    suspend fun <T, K> produceSqsMessage(
        registryKey: K,
        producerKey: T,
        produceFn: () -> Pair<String?, String?>
    ) {
        when (val registry = registries.find { it.key == registryKey }) {
            is SqsRegistry ->
                scope.launch { coroutineScope { registry.produce(producerKey, produceFn) } }
            else -> {}
        }
    }

    suspend fun start() {
        if (!consumptionStarted) {
            consumptionStarted = true
            registries.forEach { scope.launch { it.startConsumption() } }
        }
    }

    suspend fun stop() {
        if (consumptionStarted) {
            consumptionStarted = false
            registries.forEach { scope.launch { it.stopConsumption() } }
        }
    }

    class Configuration : Config {
        var numOfThreads: Int = 4
    }
}
