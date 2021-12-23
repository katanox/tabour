package com.katanox.tabour.integration.sqs.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration(proxyBeanMethods = false)
class SqsConfiguration(@Autowired val sqsProperties: SqsProperties) {

    @Bean
    @Primary
    fun amazonSQSAsync(): SqsAsyncClient {
        return SqsAsyncClient.builder()
            .credentialsProvider(credentialsProvider())
            .region(Region.of(sqsProperties.region))
            .build()
    }

    @Bean
    fun credentialsProvider(): AwsCredentialsProvider {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(sqsProperties.accessKey, sqsProperties.secretKey) as AwsCredentials
        )
    }
}
