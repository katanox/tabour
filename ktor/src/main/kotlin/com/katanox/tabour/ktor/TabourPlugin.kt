package com.katanox.tabour.ktor

import com.katanox.tabour.Tabour
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
    var tabour = tabour { numOfThreads = 1 }
    var enabled: Boolean = false
    var configure: (Tabour) -> Tabour = { it }
}

@OptIn(InternalAPI::class)
val TabourPlugin =
    createApplicationPlugin(name = "TabourPlugin", createConfiguration = ::TabourConfiguration) {
        val lock = ReentrantLock()

        on(MonitoringEvent(ApplicationStarted)) { application ->
            if (pluginConfig.enabled) {
                application.log.info("Starting tabour")

                pluginConfig.configure(pluginConfig.tabour)

                lock.withLock { pluginConfig.tabour.start() }
            }
        }
        on(MonitoringEvent(ApplicationStopped)) { application ->
            if (pluginConfig.enabled) {
                application.log.info("Stopping tabour")

                if (pluginConfig.enabled) {
                    lock.withLock { pluginConfig.tabour.stop() }
                }

                application.monitor.unsubscribe(ApplicationStarted) {}
                application.monitor.unsubscribe(ApplicationStopped) {}
            }
        }
    }
