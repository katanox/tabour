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
    open fun setupTabour(context: ConfigurableApplicationContext): Tabour {
        val annotatedBeans: Map<String, Any> =
            context.getBeansWithAnnotation(AutoconfigureTabour::class.java)

        // only one class should be annotated with this annotation
        return if (annotatedBeans.isNotEmpty() && annotatedBeans.size == 1) {
            val mainClass = ClassUtils.getUserClass(annotatedBeans.values.first())

            val beanFactory = context.beanFactory

            val threads =
                context.environment.getProperty("tabour.config.num-of-threads", Int::class.java)
                    ?: 2

            constructTabourContainer(mainClass, threads) {
                beanFactory.getBeansOfType(Registry::class.java).values.toList()
            }
        } else {
            tabourBean(emptyList(), 1)
        }
    }
}

internal fun <T> constructTabourContainer(
    c: Class<T>,
    numOfThreads: Int = 1,
    registriesProvider: () -> List<Registry<*>>
): Tabour {
    val annotation = c.getAnnotation(AutoconfigureTabour::class.java)

    return if (annotation != null) {
        tabourBean(registriesProvider(), numOfThreads)
    } else {
        tabourBean(emptyList(), numOfThreads)
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
