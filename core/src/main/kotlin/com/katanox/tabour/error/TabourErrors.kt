package com.katanox.tabour.error

sealed interface ProductionResourceNotFound

data class RegistryNotFound<T>(val registryKey: T) : ProductionResourceNotFound

data class ProducerNotFound<T>(val producerKey: T) : ProductionResourceNotFound
