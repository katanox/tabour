package com.katanox.tabour

class Tabour {
    private val registries: MutableList<Registry> = mutableListOf()

    fun register(registry: Registry): Tabour {
        registries.add(registry)
        return this
    }

    suspend fun start() {
        registries.forEach { it.startConsumption() }
    }
}
