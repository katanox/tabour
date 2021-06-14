package com.katanox.tabour.integration.sqs.config

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration(proxyBeanMethods = false)
@EnableSqs
class SqsConfiguration(@Autowired val sqsProperties: SqsProperties) {

    @Bean
    @Primary
    fun amazonSQSAsync(): AmazonSQSAsync {
        return AmazonSQSAsyncClientBuilder.standard()
            .withCredentials(credentialsProvider())
            .withRegion(sqsProperties.region)
            .build()
    }

    @Bean
    fun credentialsProvider(): AWSCredentialsProvider {
        return AWSStaticCredentialsProvider(BasicAWSCredentials(sqsProperties.accessKey, sqsProperties.secretKey))
    }

    @Bean
    fun queueMessagingTemplate(): QueueMessagingTemplate {
        return QueueMessagingTemplate(amazonSQSAsync())
    }


}