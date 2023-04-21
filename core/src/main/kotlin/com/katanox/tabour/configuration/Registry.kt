package com.katanox.tabour.configuration

interface Registry {
    suspend fun startConsumption()
    suspend fun stopConsumption()
    val key: String
}

