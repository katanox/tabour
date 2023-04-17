package com.katanox.tabour.configuration

import kotlinx.coroutines.CoroutineDispatcher

interface Registry {
    suspend fun startConsumption()
}
