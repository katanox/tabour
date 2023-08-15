package com.katanox.tabour.spring.config

import com.katanox.tabour.Tabour
import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.configuration.core.tabour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

val scope = CoroutineScope(Dispatchers.IO)

@Component
open class TabourStartupApplicationListener : ApplicationListener<ApplicationReadyEvent> {
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val annotatedBeans: Map<String, Any> =
            event.applicationContext.getBeansWithAnnotation(TabourAutoConfiguration::class.java)

        // only one class should be annotated with this class
        if (annotatedBeans.isNotEmpty() && annotatedBeans.size == 1) {
            val mainClass = ClassUtils.getUserClass(annotatedBeans.values.first())

            val annotation = mainClass.getAnnotation(TabourAutoConfiguration::class.java)

            if (annotation?.enable == true) {
                val beanFactory = event.applicationContext.beanFactory
                val registries = beanFactory.getBeansOfType(Registry::class.java).values.toList()
                val threads =
                    event.applicationContext.environment.getProperty(
                        "tabour.config.numOfThreads",
                        Int::class.java
                    )
                        ?: 2

                if (registries.isNotEmpty()) {
                    beanFactory.initializeBean(
                        tabourBean(registries, threads),
                        Tabour::javaClass.name
                    )
                }
            }
        }
    }
}

private fun tabourBean(registries: List<Registry<*>>, threads: Int): Tabour {
    val container = tabour { this.numOfThreads = threads }

    registries.fold(container) { tabourContainer, registry ->
        tabourContainer.apply { this.register(registry) }
    }

    scope.launch { container.start() }

    return container
}
