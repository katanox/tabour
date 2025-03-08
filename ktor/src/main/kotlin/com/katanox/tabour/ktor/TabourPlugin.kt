package com.katanox.tabour.ktor

import com.katanox.tabour.Tabour
import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.configuration.core.tabour
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.locks.ReentrantLock
import io.ktor.utils.io.locks.withLock

class TabourConfiguration {
    var registries: List<Registry<*>> = emptyList()
    var numOfThreads: Int = 1
    var enabled: Boolean = false
}

@OptIn(InternalAPI::class)
val TabourPlugin =
    createApplicationPlugin(name = "TabourPlugin", createConfiguration = ::TabourConfiguration) {
        var t: Tabour? = null
        val lock = ReentrantLock()

        on(MonitoringEvent(ApplicationStarted)) { application ->
            if (pluginConfig.enabled) {
                application.log.info("Starting tabour")

                lock.withLock {
                    val tb =
                        tabour { numOfThreads = pluginConfig.numOfThreads }
                            .apply { pluginConfig.registries.forEach { register(it) } }
                    t = tb
                    t?.start()
                }
            }
        }
        on(MonitoringEvent(ApplicationStopped)) { application ->
            if (pluginConfig.enabled) {
                application.log.info("Stopping tabour")

                val tb = t

                if (pluginConfig.enabled && tb != null) {
                    lock.withLock { tb.stop() }
                }

                application.monitor.unsubscribe(ApplicationStarted) {}
                application.monitor.unsubscribe(ApplicationStopped) {}
            }
        }
    }
