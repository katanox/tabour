package com.katanox.tabour

import config.Consumer
import config.IntegrationType

sealed interface Registry {
   val type: IntegrationType
}

class SqsRegistry : Registry {
    override val type: IntegrationType = IntegrationType.SQS

    private val consumers: MutableList<Consumer> = mutableListOf()

    fun addSqsConsumer(consumer: Consumer): SqsRegistry {
        consumers.add(consumer)
        consumer.onSuccess<List<Int>>(emptyList())
        return this
    }

    fun consumers(): List<Consumer> = consumers.toList()
}

class Tabour {
    private val registries: MutableList<Registry> = mutableListOf()

    fun register(registry: Registry): Tabour {
        registries.add(registry)
        return this
    }

    fun registries(): List<Registry> = registries.toList()
}
