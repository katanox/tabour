package com.katanox.tabour.configuration.core

import com.katanox.tabour.Tabour
import com.katanox.tabour.consumption.Config
import com.katanox.tabour.error.ProductionResourceNotFound

/** Creates a new [Tabour] instance */
fun tabour(init: Tabour.Configuration.() -> Unit): Tabour =
    Tabour(config(Tabour.Configuration(), init))

internal fun <T : Config> config(conf: T, init: T.() -> Unit): T = conf.apply { init() }

data class DataProductionConfiguration<T>(
    /** The function responsible to return the data that should be produced */
    val produceData: () -> T,
    /** In case of the Producer not being found, this function is invoked */
    val resourceNotFound: (ProductionResourceNotFound) -> Unit,
)
