package com.katanox.tabour.spring.config

import com.katanox.tabour.Tabour
import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.configuration.core.tabour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

val scope = CoroutineScope(Dispatchers.IO)

@Component
class ContextRefreshedEventListener(
    @Value("\${tabour.config.num-of-threads:2}") val threadsCount: Int
) : ApplicationListener<ContextRefreshedEvent?> {
    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent?) {
        if (contextRefreshedEvent?.applicationContext != null) {
            setupTabour(contextRefreshedEvent.applicationContext)
        }
    }

    @Bean @Lazy(false) fun tabourBean(): Tabour = tabourSetup(emptyList(), threadsCount)

    private fun setupTabour(context: ApplicationContext) {
        val annotatedBeans: Map<String, Any> =
            context.getBeansWithAnnotation(AutoconfigureTabour::class.java)

        // only one class should be annotated with this annotation
        if (annotatedBeans.isNotEmpty() && annotatedBeans.size == 1) {
            val mainClass = ClassUtils.getUserClass(annotatedBeans.values.first())

            val tabourContainers = context.getBeansOfType(Tabour::class.java)

            val annotation = mainClass.getAnnotation(AutoconfigureTabour::class.java)

            if (annotation != null && tabourContainers.size == 1) {
                val container = tabourContainers.values.toList().first()

                container.updateRegistries(
                    context.getBeansOfType(Registry::class.java).values.toList()
                )
            }
        }
    }
}

private fun tabourSetup(registries: List<Registry<*>>, threads: Int): Tabour {
    val container = tabour { this.numOfThreads = threads }

    registries.fold(container) { tabourContainer, registry ->
        tabourContainer.apply { this.register(registry) }
    }

    if (registries.isNotEmpty()) {
        scope.launch { container.start() }
    }

    return container
}
