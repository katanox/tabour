package com.katanox.tabour.spring.config

import com.katanox.tabour.Tabour
import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.configuration.core.tabour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.util.ClassUtils

val scope = CoroutineScope(Dispatchers.IO)

@AutoConfiguration
open class TabourConfigurer {

    @Bean
    @Lazy(false)
    open fun t(context: ConfigurableApplicationContext): Tabour {
        val annotatedBeans: Map<String, Any> =
            context.getBeansWithAnnotation(AutoconfigureTabour::class.java)

        // only one class should be annotated with this annotation
        return if (annotatedBeans.isNotEmpty() && annotatedBeans.size == 1) {
            val mainClass = ClassUtils.getUserClass(annotatedBeans.values.first())

            val annotation = mainClass.getAnnotation(AutoconfigureTabour::class.java)

            val beanFactory = context.beanFactory

            if (annotation != null) {
                val registries = beanFactory.getBeansOfType(Registry::class.java).values.toList()
                val threads =
                    context.environment.getProperty("tabour.config.num-of-threads", Int::class.java)
                        ?: 2

                if (registries.isNotEmpty()) {
                    tabourBean(registries, threads)
                } else {
                    tabourBean(emptyList(), 0)
                }
            } else {
                tabourBean(emptyList(), 0)
            }
        } else {
            tabourBean(emptyList(), 0)
        }
    }
}

private fun tabourBean(registries: List<Registry<*>>, threads: Int): Tabour {
    val container = tabour { this.numOfThreads = threads }

    registries.fold(container) { tabourContainer, registry ->
        tabourContainer.apply { this.register(registry) }
    }

    if (registries.isNotEmpty()) {
        scope.launch { container.start() }
    }

    return container
}
