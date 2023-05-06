package com.katanox.tabour.configuration

interface Registry<K> {
    suspend fun startConsumption()
    suspend fun stopConsumption()
    val key: K
}
