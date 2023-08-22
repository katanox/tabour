package com.katanox.tabour

import com.katanox.tabour.configuration.core.tabour
import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.configuration.sqs.sqsRegistry
import com.katanox.tabour.configuration.sqs.sqsRegistryConfiguration
import com.katanox.tabour.sqs.production.FifoQueueData
import com.katanox.tabour.sqs.production.NonFifoQueueData
import java.net.URL
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
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
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
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
    private lateinit var nonFifoQueueUrl: String
    private lateinit var fifoQueueUrl: String
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

        nonFifoQueueUrl =
            sqsClient
                .createQueue(CreateQueueRequest.builder().queueName("my-queue").build())
                .queueUrl()

        fifoQueueUrl =
            sqsClient
                .createQueue(
                    CreateQueueRequest.builder()
                        .attributes(
                            mutableMapOf(
                                QueueAttributeName.FIFO_QUEUE to "TRUE",
                                QueueAttributeName.CONTENT_BASED_DEDUPLICATION to "TRUE",
                            )
                        )
                        .queueName("my-queue.fifo")
                        .build()
                )
                .queueUrl()

        scope.launch { container.start() }
    }

    @AfterAll
    fun cleanup() {
        scope.launch { container.stop() }
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(nonFifoQueueUrl).build())
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(fifoQueueUrl).build())
    }

    @AfterEach
    fun cleanupEach() {
        purgeQueue(nonFifoQueueUrl)
        purgeQueue(fifoQueueUrl)
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

            val producer =
                sqsProducer(URL(nonFifoQueueUrl), "test-producer") { onError = { println(it) } }

            sqsRegistry.addProducer(producer)
            container.register(sqsRegistry)

            container.produceSqsMessage("test-registry", "test-producer") {
                NonFifoQueueData("this is a test message")
            }

            await
                .withPollInterval(Duration.ofMillis(500))
                .timeout(Duration.ofSeconds(5))
                .untilAsserted {
                    val receiveMessagesResponse =
                        sqsClient.receiveMessage(
                            ReceiveMessageRequest.builder()
                                .queueUrl(nonFifoQueueUrl)
                                .maxNumberOfMessages(5)
                                .build()
                        )

                    assertTrue(receiveMessagesResponse.messages().isNotEmpty())
                    assertEquals(
                        receiveMessagesResponse.messages().first().body(),
                        "this is a test message"
                    )
                }
        }

    @Test
    fun `produce a message to a fifo queue`() =
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

            val producer =
                sqsProducer(URL(fifoQueueUrl), "test-producer") { onError = { println(it) } }

            sqsRegistry.addProducer(producer)
            container.register(sqsRegistry)

            container.produceSqsMessage("test-registry", "test-producer") {
                FifoQueueData("this is a fifo test message", "group1")
            }

            await
                .withPollInterval(Duration.ofMillis(500))
                .timeout(Duration.ofSeconds(5))
                .untilAsserted {
                    val receiveMessagesResponse =
                        sqsClient.receiveMessage(
                            ReceiveMessageRequest.builder()
                                .queueUrl(fifoQueueUrl)
                                .maxNumberOfMessages(5)
                                .build()
                        )

                    assertTrue(receiveMessagesResponse.messages().isNotEmpty())
                    assertEquals(
                        receiveMessagesResponse.messages().first().body(),
                        "this is a fifo test message"
                    )
                }
        }

    private fun purgeQueue(url: String) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build())
    }
}
