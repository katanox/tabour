package com.katanox.tabour.integration.sqs.config

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandler
import com.katanox.tabour.integration.sqs.core.consumer.SqsEventHandlerRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "tabour.sqs", name = ["enable-consumption"], havingValue = "true")
class SqsConsumptionConfiguration(@Autowired val sqsProperties: SqsProperties) {

    @Bean
    fun simpleMessageListenerContainerFactory(
        amazonSQSAsync: AmazonSQSAsync
    ): SimpleMessageListenerContainerFactory? {
        val factory = SimpleMessageListenerContainerFactory()
        factory.setAmazonSqs(amazonSQSAsync)
        factory.setAutoStartup(sqsProperties.autoStartup)
        factory.setMaxNumberOfMessages(sqsProperties.maxNumberOfMessages)
        factory.setTaskExecutor(createDefaultTaskExecutor())
        return factory
    }

    protected fun createDefaultTaskExecutor(): AsyncTaskExecutor? {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.setThreadNamePrefix("SQS-")
        threadPoolTaskExecutor.corePoolSize = sqsProperties.corePoolSize
        threadPoolTaskExecutor.maxPoolSize = sqsProperties.maxPoolSize
        threadPoolTaskExecutor.setQueueCapacity(sqsProperties.queueCapacity)
        threadPoolTaskExecutor.afterPropertiesSet()
        return threadPoolTaskExecutor
    }

    @Bean
    fun sqsMessageHandlerRegistry(
        registrations: List<SqsEventHandler>
    ): SqsEventHandlerRegistry {
        return SqsEventHandlerRegistry(registrations)
    }

    @Bean
    fun sqsLifecycle(registry: SqsEventHandlerRegistry): SqsAutoConfigurationLifecycle {
        return SqsAutoConfigurationLifecycle(registry)
    }
}