package com.katanox.tabour

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.sqs.SqsRegistry
import kotlinx.coroutines.*

class Tabour(numOfThreads: Int = 4) {
    private val registries: MutableSet<Registry> = mutableSetOf()
    @OptIn(DelicateCoroutinesApi::class)
    val dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(numOfThreads, "tabour")
    private var consumptionStarted: Boolean = false
    private val scope = CoroutineScope(dispatcher)

    fun register(registry: Registry): Tabour = this.apply { registries.add(registry) }

    suspend fun produce(registryKey: String, producerKey: String) {
        registries
            .find { it.key == registryKey }
            ?.also {
                when (it) {
                    is SqsRegistry -> scope.launch { it.produce(producerKey) }
                    else -> {}
                }
            }
    }

    suspend fun start() {
        if (!consumptionStarted) {
            consumptionStarted = true
            registries.forEach { scope.launch { it.startConsumption() } }
        }
    }
}
