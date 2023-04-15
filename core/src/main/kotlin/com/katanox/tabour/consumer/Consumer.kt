package com.katanox.tabour.consumer

// import com.amazonaws.services.sqs.AmazonSQSAsync
// import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
// import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import config.SqsConfiguration
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@OptIn(DelicateCoroutinesApi::class)
val scope = CoroutineScope(context = newFixedThreadPoolContext(16, "sqs-poller"))

class SqsPoller(credentialsProvider: AwsCredentialsProvider) {
    private val sqs: SqsAsyncClient = SqsAsyncClient.builder()
        .credentialsProvider(credentialsProvider)
//        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("", "")))
        .build()

    suspend fun start(consumers: List<SqsConfiguration>) {
        consumers.forEach { scope.launch { accept(it) } }
    }

    private suspend fun accept(configuration: SqsConfiguration) {
        while (true) {
            repeat(configuration.workers) {
                scope.launch {
                    val request =
                        ReceiveMessageRequest.builder()
                            .queueUrl(configuration.url)
                            .maxNumberOfMessages(configuration.maxMessages)
                            .waitTimeSeconds(configuration.waitTime)
                            .build()

                    try {
                        sqs.receiveMessage(request).await().let { response ->
                            configuration.successFn(response.messages())
                        }
                    } catch (e: Exception) {
                        configuration.onFail(e)
                    }
                }
            }

            delay(configuration.sleepTime.seconds)
        }
    }

    private fun messages(ids: List<String>) {}
}
