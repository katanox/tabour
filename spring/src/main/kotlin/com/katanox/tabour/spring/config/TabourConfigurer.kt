package com.katanox.tabour.spring.config

import com.katanox.tabour.Tabour
import com.katanox.tabour.configuration.Registry
import com.katanox.tabour.configuration.core.tabour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

val scope = CoroutineScope(Dispatchers.IO)

class TabourConfigurer

@Component
class ContextRefreshedEventListener(
    @Value("\${tabour.config.num-of-threads:2}") val threadsCount: Int,
    @Value("\${tabour.config.enabled:true}") val enabled: Boolean
) : ApplicationListener<ContextRefreshedEvent?> {
    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent?) {
        if (contextRefreshedEvent?.applicationContext != null) {
            setupTabour(contextRefreshedEvent.applicationContext)
        }
    }

    @Bean @Lazy(false) fun tabourBean(): Tabour = tabour { this.numOfThreads = threadsCount }

    private fun setupTabour(context: ApplicationContext) {
        val annotatedBeans: Map<String, Any> =
            context.getBeansWithAnnotation(AutoconfigureTabour::class.java)

        // only one class should be annotated with this annotation
        if (annotatedBeans.isNotEmpty() && annotatedBeans.size == 1) {
            val mainClass = ClassUtils.getUserClass(annotatedBeans.values.first())

            val tabourContainers = context.getBeansOfType(Tabour::class.java)

            if (tabourContainers.size == 1 && enabled) {
                launchTabour(mainClass, tabourContainers.values.first()) {
                    context.getBeansOfType(Registry::class.java).values.toList()
                }
            }
        }
    }
}

@Component
class TabourDisposer(private val tabour: Tabour) : DisposableBean {
    override fun destroy() {
        tabour.stop()
    }
}

/**
 * starts the tabour container only if the annotation [AutoconfigureTabour] is present and there are
 * [Registry] instances available in the spring context
 */
internal fun launchTabour(
    mainClass: Class<*>,
    tabourContainer: Tabour,
    registriesProvider: () -> List<Registry<*>>
) {
    val annotation = mainClass.getAnnotation(AutoconfigureTabour::class.java)

    if (annotation != null) {
        val registries = registriesProvider()

        if (registries.isNotEmpty()) {
            registries
                .fold(tabourContainer) { container, registry -> container.register(registry) }
                .apply {
                    val container = this
                    scope.launch { container.start() }
                }
        }
    }
}
