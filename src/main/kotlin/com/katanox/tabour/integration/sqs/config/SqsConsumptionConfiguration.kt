package com.katanox.tabour.integration.sqs.config

import com.katanox.tabour.config.EventPollerProperties
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.factory.BusType
import com.katanox.tabour.factory.EventHandlerFactory
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandler
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandlerRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "tabour.sqs", name = ["enable-consumption"], havingValue = "true")
class SqsConsumptionConfiguration(
    val tabourAutoConfigs: TabourAutoConfigs,
) {

    @Bean
    @Suppress("UNCHECKED_CAST")
    fun sqsMessageHandlerRegistry(
        eventConsumerFactory: EventHandlerFactory,
        eventPollerProperties: EventPollerProperties,
        sqsConfiguration: SqsConfiguration,
    ): SqsEventHandlerRegistry {
        return SqsEventHandlerRegistry(
            eventHandlers = eventConsumerFactory.getEventHandlers(BusType.SQS) as List<SqsEventHandler>,
            sqsConfiguration = sqsConfiguration,
            pollerConfigs = eventPollerProperties,
            tabourAutoConfigs = tabourAutoConfigs
        )
    }

    @Bean
    fun sqsLifecycle(registry: SqsEventHandlerRegistry): SqsAutoConfigurationLifecycle {
        return SqsAutoConfigurationLifecycle(registry)
    }
}
