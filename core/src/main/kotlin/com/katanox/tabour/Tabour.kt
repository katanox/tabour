package com.katanox.tabour

import com.katanox.tabour.consumption.Config
import com.katanox.tabour.error.RegistryNotFound
import com.katanox.tabour.sqs.SqsRegistry
import com.katanox.tabour.sqs.production.SqsDataProductionConfiguration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

const val TABOUR_SHUTDOWN_MESSAGE = "tabour shutdown"

/**
 * After registering all registries, the tabour container can be started using [start].
 *
 * To stop the consumption of the container, you can use [stop]
 *
 * By default, the container will allocate 4 threads which can be configured using [Configuration]
 */
class Tabour internal constructor(val config: Configuration) {
    private val registries: MutableSet<SqsRegistry<*>> = mutableSetOf()

    private val registriesByKey by lazy { registries.associateBy { it.key } }

    private var consumptionStarted: Boolean = false

    private val scope =
        CoroutineScope(config.dispatcher + config.coroutineContext).also {
            println(config.dispatcher)
        }

    /**
     * Adds a new registry to the Tabour Container. All registries must be registered before
     * starting the tabour container.
     */
    fun <T> register(registry: SqsRegistry<T>): Tabour = apply { registries.add(registry) }

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
    fun <T, K> produceMessage(
        registryKey: K,
        producerKey: T,
        productionConfiguration: SqsDataProductionConfiguration,
    ) {
        when (val registry = registriesByKey[registryKey]) {
            is SqsRegistry ->
                scope.launch { registry.produce(producerKey, productionConfiguration) }
            else -> productionConfiguration.resourceNotFound(RegistryNotFound(registryKey))
        }
    }

    /** Starts the consumers of the registered registries. */
    fun start() {
        if (!consumptionStarted) {
            consumptionStarted = true
            registries.forEach { scope.launch { it.startConsumption() } }
        }
    }

    /** Stops the consumers of the registered registries. */
    suspend fun stop() {
        if (consumptionStarted) {
            consumptionStarted = false
            registries.forEach { it.stopConsumption() }
            scope.cancel(TABOUR_SHUTDOWN_MESSAGE)
        }
    }

    /** Indicates if tabour has started all the consumers */
    fun running(): Boolean = consumptionStarted

    class Configuration : Config {
        /** The thread pool size for tabour. Default is 1 */
        var numOfThreads: Int = 1

        var coroutineContext: CoroutineContext = EmptyCoroutineContext

        @OptIn(DelicateCoroutinesApi::class)
        var dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(numOfThreads, "tabour")
    }
}
