package com.katanox.tabour.config

import com.katanox.tabour.base.IEventPublisherBase
import com.katanox.tabour.factory.EventHandlerFactory
import com.katanox.tabour.factory.EventPublisherFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan("com.katanox.tabour")
class TabourAutoConfigs(@Autowired val tabourProperties: TabourProperties) {
    @Bean
    fun eventConsumerFactory(): EventHandlerFactory {
        return EventHandlerFactory(this)
    }

    @Bean
    fun eventPublisherFactory(@Autowired services: List<IEventPublisherBase>): EventPublisherFactory {
        return EventPublisherFactory(services)
    }
}
