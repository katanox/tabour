package com.katanox.tabour.integration.sqs.config

import com.katanox.tabour.config.EventHandlerProperties
import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.factory.EventConsumerFactory
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventFetcher
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandler
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandlerRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "tabour.sqs", name = ["enable-consumption"], havingValue = "true")
class SqsConsumptionConfiguration(@Autowired val sqsProperties: SqsProperties) {

    @Bean
    fun sqsMessageHandlerRegistry(
        eventConsumerFactory: EventConsumerFactory,
        eventHandlerProperties: EventHandlerProperties,
        eventPollerProperties: EventPollerProperties,
        sqsConfiguration: SqsConfiguration,
    ): SqsEventHandlerRegistry {
        return SqsEventHandlerRegistry(
            eventConsumerFactory.getEventConsumers(BusType.SQS) as List<SqsEventHandler>,
            eventPollerProperties,
            sqsConfiguration,
            sqsFetcher(eventPollerProperties, sqsConfiguration)
        )
    }

    @Bean
    fun sqsLifecycle(registry: SqsEventHandlerRegistry): SqsAutoConfigurationLifecycle {
        return SqsAutoConfigurationLifecycle(registry)
    }

    @Bean
    fun sqsFetcher(
        eventPollerProperties: EventPollerProperties,
        sqsConfiguration: SqsConfiguration,
    ): SqsEventFetcher {
        return SqsEventFetcher(sqsConfiguration, eventPollerProperties)
    }
}
