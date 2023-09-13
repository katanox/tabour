package com.katanox.tabour

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.sqs.SqsRegistry
import com.katanox.tabour.sqs.production.SqsDataForProduction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

/**
 * A container of a collection of [Registry]. This class is the entrypoint for interacting with
 * consumers and producers.
 *
 * Consumers and producers are added to a [Registry] and then the registry itself must be added to a
 * tabour container
 *
 * After registering all registries, the tabour container can be started using [start].
 *
 * To stop the consumption of the container, you can use [stop]
 *
 * By default, the container will allocate 4 threads which can be configured using [Configuration]
 */
class Tabour internal constructor(val config: Configuration) {
    val registries: MutableSet<Registry<*>> = mutableSetOf()

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher =
        newFixedThreadPoolContext(config.numOfThreads, "tabour")

    private var consumptionStarted: Boolean = false

    private val scope = CoroutineScope(dispatcher)

    /**
     * Adds a new registry to the Tabour Container
     *
     * All registries must be registered before starting the tabour container.
     */
    fun <T> register(registry: Registry<T>): Tabour = this.apply { registries.add(registry) }

    fun updateRegistries(registries: Iterable<Registry<*>>) =
        this.apply {
            this.registries.clear()
            this.registries.addAll(registries)
        }

    /**
     * Uses a registered SqsProducer to produce a message. The producer must be part of a sqs
     * registry that has been itself registered
     *
     * [registryKey]: the key of the registry which the producer is part of
     *
     * [producerKey]: the key of the producer itself
     *
     * [produceFn]: A function that returns a Pair<String?, String>. The first part of the pair is
     * the body of the message and the second part is the message group id. If the body is null, a
     * message is not produced
     *
     * Note: If the registry is not found (either wrong Registry or Producer key), nothing happens.
     */
    suspend fun <T, K> produceMessage(
        registryKey: K,
        producerKey: T,
        produceFn: () -> SqsDataForProduction
    ) {
        when (val registry = registries.find { it.key == registryKey }) {
            is SqsRegistry ->
                scope.launch { coroutineScope { registry.produce(producerKey, produceFn) } }
            else -> {}
        }
    }

    /** Starts the consumers of the registered registries. */
    suspend fun start() {
        if (!consumptionStarted) {
            consumptionStarted = true
            registries.forEach { scope.launch { it.startConsumption() } }
        }
    }
    /** Stops the consumers of the registered registries. */
    suspend fun stop() {
        if (consumptionStarted) {
            consumptionStarted = false
            registries.forEach { scope.launch { it.stopConsumption() } }
        }
    }

    fun running(): Boolean = consumptionStarted

    class Configuration : Config {
        var numOfThreads: Int = 4
    }
}
