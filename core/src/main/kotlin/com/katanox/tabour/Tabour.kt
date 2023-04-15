package com.katanox.tabour

import com.katanox.tabour.registry.Registry

class Tabour {
    private val registries: MutableList<Registry> = mutableListOf()

    fun register(registry: Registry): Tabour {
        registries.add(registry)
        return this
    }

    suspend fun start() {
        println(registries.size)
        registries.forEach { it.startConsumption() }
    }
}
