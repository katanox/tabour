package com.katanox.tabour.configuration.core

import com.katanox.tabour.Tabour
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.error.ProductionResourceNotFound

/** Creates a new [Tabour] instance */
fun tabour(init: Tabour.Configuration.() -> Unit): Tabour =
    Tabour(config(Tabour.Configuration(), init))

internal fun <T : Config> config(conf: T, init: T.() -> Unit): T = conf.apply { init() }

data class DataProductionConfiguration<T, K>(
    val produceData: () -> T,
    val dataProduced: (T, K) -> Unit,
    val resourceNotFound: (ProductionResourceNotFound) -> Unit
)
