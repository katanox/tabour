package com.katanox.tabour

import com.katanox.tabour.configuration.Registry
import kotlinx.coroutines.*

class Tabour(numOfThreads: Int = 4) {
    private val registries: MutableSet<Registry> = mutableSetOf()

    @OptIn(DelicateCoroutinesApi::class)
    val dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(numOfThreads, "tabour")

    private val scope = CoroutineScope(dispatcher)

    fun register(registry: Registry): Tabour = this.apply { registries.add(registry) }

    suspend fun start() {
        registries.forEach { scope.launch { it.startConsumption() } }
    }
}
