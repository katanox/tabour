package com.katanox.tabour.config


import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.factory.EventConsumerFactory
import com.katanox.tabour.factory.EventPublisherFactory
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan("com.katanox.tabour")
class TabourAutoConfigs(
    @Autowired val tabourProperties: TabourProperties,
) {

    @Bean
    fun retryRegistry(): RetryRegistry {
        val retryConfig =
            RetryConfig.custom<Any>()
                .maxAttempts(tabourProperties.retryMaxCount)
                .intervalFunction(IntervalFunction.ofExponentialBackoff())
                .build()
        return RetryRegistry.of(retryConfig)
    }

    @Bean
    fun eventConsumerFactory(): EventConsumerFactory {
        return EventConsumerFactory(this)
    }

    @Bean
    fun eventPublisherFactory(@Autowired services: List<IEventPublisherBase>): EventPublisherFactory {
        return EventPublisherFactory(services)
    }

}
