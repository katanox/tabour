package com.katanox.tabour.configuration

interface Registry {
    suspend fun startConsumption()
    val key: String
}

