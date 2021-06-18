package com.katanox.tabour.config


import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration(proxyBeanMethods = false)
class TabourAutoConfigs(@Autowired val tabourProperties: TabourProperties) {

    @Bean
    fun retryRegistry(): RetryRegistry {
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(tabourProperties.retryMaxCount)
            .intervalFunction(IntervalFunction.ofExponentialBackoff())
            .build()
        return RetryRegistry.of(retryConfig)
    }

}


