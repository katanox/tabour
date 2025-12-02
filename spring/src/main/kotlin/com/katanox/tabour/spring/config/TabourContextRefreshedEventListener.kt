package com.katanox.tabour.spring.config

import com.katanox.tabour.Tabour
import com.katanox.tabour.configuration.core.tabour
import com.katanox.tabour.sqs.SqsRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

/** @suppress */
@Component
class TabourContextRefreshedEventListener(
    @Value("\${tabour.config.num-of-threads:4}") val threadsCount: Int,
    @Value("\${tabour.config.enabled:true}") val enabled: Boolean,
) : ApplicationListener<ContextRefreshedEvent?> {
    private val scope = CoroutineScope(Dispatchers.IO)

    @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent?) {
        if (contextRefreshedEvent?.applicationContext != null) {
            setupTabour(contextRefreshedEvent.applicationContext)
        }
    }

    @Bean @Lazy(false) fun tabourBean(): Tabour = tabour { this.numOfThreads = threadsCount }

    private fun launchTabour(
        mainClass: Class<*>,
        tabourContainer: Tabour,
        registriesProvider: () -> List<SqsRegistry<*>>,
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

    private fun setupTabour(context: ApplicationContext) {
        val annotatedBeans: Map<String, Any> =
            context.getBeansWithAnnotation(AutoconfigureTabour::class.java)

        // only one class should be annotated with this annotation
        if (annotatedBeans.isNotEmpty() && annotatedBeans.size == 1) {
            val mainClass = ClassUtils.getUserClass(annotatedBeans.values.first())

            val tabourContainers = context.getBeansOfType(Tabour::class.java)

            if (tabourContainers.size == 1 && enabled) {
                launchTabour(mainClass, tabourContainers.values.first()) {
                    context.getBeansOfType(SqsRegistry::class.java).values.toList()
                }
            }
        }
    }
}

/** @suppress */
@Component
class TabourDisposer(private val tabour: Tabour) : DisposableBean {
    override fun destroy() {
        runBlocking { tabour.stop() }
    }
}
