package com.katanox.tabour

import com.katanox.tabour.configuration.core.tabour
import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.configuration.sqs.sqsRegistry
import com.katanox.tabour.configuration.sqs.sqsRegistryConfiguration
import com.katanox.tabour.sqs.production.NonFifoQueueData
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TabourTest {
    private val localstack =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:2.2.0"))
            .withServices(LocalStackContainer.Service.SQS)
            .withReuse(true)

    private val credentials = AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey)
    private val container = tabour { numOfThreads = 1 }
    private lateinit var sqsClient: SqsClient
    private lateinit var queueUrl: String
    private val scope = CoroutineScope(Dispatchers.IO)

    @BeforeAll
    fun setup() {
        localstack.start()

        sqsClient =
            SqsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(Region.of(localstack.region))
                .build()

        queueUrl =
            sqsClient
                .createQueue(CreateQueueRequest.builder().queueName("my-queue").build())
                .queueUrl()

        scope.launch { container.start() }
    }

    @AfterAll
    fun cleanup() {
        //        localstack.stop()
        scope.launch { container.stop() }
        //
        // sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build())
    }

    @Test
    fun `produce a message to a non fifo queue`() =
        runTest(UnconfinedTestDispatcher()) {
            val config =
                sqsRegistryConfiguration(
                    "test-registry",
                    StaticCredentialsProvider.create(credentials),
                    Region.of(localstack.region)
                ) {
                    this.endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                }

            val sqsRegistry = sqsRegistry(config)

            val producer = sqsProducer(URL(queueUrl), "test-producer") { onError = { println(it) } }

            sqsRegistry.addProducer(producer)
            container.register(sqsRegistry)

            container.produceSqsMessage("test-registry", "test-producer") {
                NonFifoQueueData("this is a test message")
            }

            val receiveMessagesResponse =
                sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .build()
                )
            //
            //        assertEquals(1, receiveMessagesResponse.messages().size)
            //        assertEquals("this is a test message",
            println(receiveMessagesResponse.messages())
        }
}
