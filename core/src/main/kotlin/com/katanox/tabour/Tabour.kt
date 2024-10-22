package com.katanox.tabour

import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.error.RegistryNotFound
import com.katanox.tabour.sqs.SqsRegistry
import com.katanox.tabour.sqs.production.SqsDataProductionConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

/**
 * A container for collection of [Registry]. This class is the entrypoint for interacting with
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
    private val registries: MutableSet<Registry<*>> = mutableSetOf()

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher =
        newFixedThreadPoolContext(config.numOfThreads, "tabour")

    private var consumptionStarted: Boolean = false

    private val scope = CoroutineScope(dispatcher)

    /**
     * Adds a new registry to the Tabour Container. All registries must be registered before
     * starting the tabour container.
     */
    fun <T> register(registry: Registry<T>): Tabour = this.apply { registries.add(registry) }

    /**
     * Produces a message using one of the registered producers
     *
     * [registryKey]: the key of the registry which the producer is part of
     *
     * [producerKey]: the key of the producer itself
     *
     * [productionConfiguration]: The object that provides the handlers to
     * - produce data
     * - callback for when the data is produced
     * - handle the case where the production is not possible because the producer is not found
     */
    suspend fun <T, K> produceMessage(
        registryKey: K,
        producerKey: T,
        productionConfiguration: SqsDataProductionConfiguration,
    ) {
        when (val registry = registries.find { it.key == registryKey }) {
            is SqsRegistry ->
                scope.launch { registry.produce(producerKey, productionConfiguration) }
            else -> productionConfiguration.resourceNotFound(RegistryNotFound(registryKey))
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
    fun stop() {
        if (consumptionStarted) {
            consumptionStarted = false
            registries.forEach { scope.launch { it.stopConsumption() } }
        }
    }

    /** Indicates if tabour has started all the consumers */
    fun running(): Boolean = consumptionStarted

    class Configuration : Config {
        /** The thread pool size for tabour. Default is 4 */
        var numOfThreads: Int = 4
    }
}
