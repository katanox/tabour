package com.katanox.tabour

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.CreateQueueRequest
import aws.sdk.kotlin.services.sqs.model.DeleteQueueRequest
import aws.sdk.kotlin.services.sqs.model.PurgeQueueRequest
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.smithy.kotlin.runtime.net.url.Url
import com.katanox.tabour.configuration.core.DataProductionConfiguration
import com.katanox.tabour.configuration.core.tabour
import com.katanox.tabour.configuration.sqs.sqsConsumer
import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.configuration.sqs.sqsRegistry
import com.katanox.tabour.configuration.sqs.sqsRegistryConfiguration
import com.katanox.tabour.error.ProducerNotFound
import com.katanox.tabour.error.ProductionResourceNotFound
import com.katanox.tabour.error.RegistryNotFound
import com.katanox.tabour.sqs.production.SqsDataProductionConfiguration
import com.katanox.tabour.sqs.production.SqsMessageProduced
import com.katanox.tabour.sqs.production.SqsProductionData
import java.net.URI
import java.net.URL
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollDelay
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TabourTest {
    private val localstack =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:4.7"))
            .withServices(LocalStackContainer.Service.SQS)
            .withReuse(true)

    private lateinit var sqsClient: SqsClient
    private lateinit var nonFifoQueueUrl: String
    private lateinit var fifoQueueUrl: String

    @BeforeAll
    fun setup() {
        localstack.start()

        sqsClient = SqsClient {
            endpointUrl =
                Url.parse(
                    localstack
                        .getEndpointOverride(LocalStackContainer.Service.SQS)
                        .toURL()
                        .toString()
                )
            region = localstack.region

            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = localstack.accessKey
                secretAccessKey = localstack.secretKey
            }
        }

        nonFifoQueueUrl = runBlocking {
            sqsClient.createQueue(CreateQueueRequest { queueName = "my-queue" }).queueUrl
                ?: fail("Queue not created")
        }

        fifoQueueUrl = runBlocking {
            sqsClient
                .createQueue(
                    CreateQueueRequest {
                        attributes =
                            mapOf(
                                QueueAttributeName.FifoQueue to "TRUE",
                                QueueAttributeName.ContentBasedDeduplication to "TRUE",
                            )
                        queueName = "my-queue.fifo"
                    }
                )
                .queueUrl ?: fail("Queue not created")
        }
    }

    @AfterAll
    fun cleanup() {
        runBlocking {
            sqsClient.deleteQueue(DeleteQueueRequest { queueUrl = nonFifoQueueUrl })
            sqsClient.deleteQueue(DeleteQueueRequest { queueUrl = fifoQueueUrl })
        }
    }

    @Test
    fun isRunningIsTrue() = runTest {
        val container = tabour { numOfThreads = 1 }
        container.start()
        assertTrue { container.running() }
        container.stop()
    }

    @Test
    fun isRunningIsFalse() = runTest {
        val container = tabour { numOfThreads = 1 }
        container.start()
        container.stop()
        assertFalse { container.running() }
    }

    @Test
    @Tag("sqs-consumer-test")
    fun `consume messages`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            var counter = 0
            val sqsProducerConfiguration =
                DataProductionConfiguration<SqsProductionData, SqsMessageProduced>(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a fifo test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { _ -> println("Resource not found") },
                )

            val producer =
                sqsProducer(URL.of(URI.create(nonFifoQueueUrl), null), "test-producer", ::println)

            val consumer =
                sqsConsumer(
                    URL.of(URI.create(nonFifoQueueUrl), null),
                    key = "my-consumer",
                    onSuccess = {
                        counter++
                        true
                    },
                    onError = ::println,
                ) {
                    this.config = sqsConsumerConfiguration {
                        sleepTime = 200.milliseconds
                        consumeWhile = { counter < 1 }
                    }
                }

            container.register(sqsRegistry.addConsumer(consumer).addProducer(producer)).start()

            container.produceMessage("test-registry", "test-producer", sqsProducerConfiguration)

            await.withPollDelay(Duration.ofSeconds(3)).untilAsserted { assertTrue { counter >= 1 } }

            purgeQueue(nonFifoQueueUrl)
            container.stop()
        }

    @Test
    @Tag("sqs-consumer-test")
    fun `consume 1000 messages`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            var counter = 0
            val sqsProducerConfiguration =
                DataProductionConfiguration<SqsProductionData, SqsMessageProduced>(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a fifo test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { _ -> println("Resource not found") },
                )

            val producer =
                sqsProducer(URL.of(URI.create(nonFifoQueueUrl), null), "test-producer", ::println)

            val consumer =
                sqsConsumer(
                    URL.of(URI.create(nonFifoQueueUrl), null),
                    key = "my-consumer",
                    onSuccess = {
                        counter++
                        println(counter)
                        true
                    },
                    onError = ::println,
                ) {
                    this.config = sqsConsumerConfiguration { sleepTime = 100.milliseconds }
                }

            container.register(sqsRegistry.addConsumer(consumer).addProducer(producer)).start()

            repeat(1000) {
                container.produceMessage("test-registry", "test-producer", sqsProducerConfiguration)
            }

            await
                .timeout(2.minutes.toJavaDuration())
                .withPollDelay(Duration.ofSeconds(1))
                .untilAsserted { assertTrue { counter == 1000 } }

            val receiveMessagesResponse =
                sqsClient.receiveMessage(
                    ReceiveMessageRequest {
                        queueUrl = nonFifoQueueUrl
                        maxNumberOfMessages = 5
                    }
                )

            assertEquals(null, receiveMessagesResponse.messages)

            purgeQueue(nonFifoQueueUrl)
            container.stop()
        }

    @Test
    @Tag("sqs-consumer-test")
    fun `consume messages with multi concurrency`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            var counter = 0

            val producer =
                sqsProducer(URL.of(URI.create(nonFifoQueueUrl), null), "test-producer") {
                    println(it)
                }
            val consumer =
                sqsConsumer(
                    URL.of(URI.create(nonFifoQueueUrl), null),
                    key = "my-consumer",
                    onSuccess = {
                        counter++
                        true
                    },
                    onError = ::println,
                ) {
                    this.config = sqsConsumerConfiguration {
                        sleepTime = 200.milliseconds
                        consumeWhile = { counter < 50 }
                        concurrency = 5
                        receiveRequestConfigurationBuilder = { maxNumberOfMessages = 2 }
                    }
                }

            sqsRegistry.addConsumer(consumer).addProducer(producer)
            container.register(sqsRegistry).start()

            val sqsProducerConfiguration =
                SqsDataProductionConfiguration(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a fifo test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { _ -> println("Resource not found") },
                )

            repeat(50) {
                container.produceMessage("test-registry", "test-producer", sqsProducerConfiguration)
            }

            await.withPollDelay(Duration.ofSeconds(2)).untilAsserted { assertTrue { counter > 1 } }
            container.stop()
        }

    @Test
    @Tag("sqs-producer-test")
    fun `produce a message to a non fifo queue`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            val sqsProducerConfiguration =
                SqsDataProductionConfiguration(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { _ -> println("Resource not found") },
                )

            val producer =
                sqsProducer(URL.of(URI.create(nonFifoQueueUrl), null), "test-producer", ::println)

            sqsRegistry.addProducer(producer)
            container.register(sqsRegistry)
            container.start()

            container.produceMessage("test-registry", "test-producer", sqsProducerConfiguration)

            await
                .withPollInterval(Duration.ofMillis(500))
                .timeout(Duration.ofSeconds(5))
                .untilAsserted {
                    val receiveMessagesResponse = runBlocking {
                        sqsClient.receiveMessage(
                            ReceiveMessageRequest {
                                queueUrl = nonFifoQueueUrl
                                maxNumberOfMessages = 5
                            }
                        )
                    }

                    assertEquals(true, receiveMessagesResponse.messages?.isNotEmpty())
                    assertEquals(
                        receiveMessagesResponse.messages?.first()?.body,
                        "this is a test message",
                    )
                }

            purgeQueue(nonFifoQueueUrl)
            container.stop()
        }

    @Test
    fun `consuming messages deletes the message from queues`() = runTest {
        val container = tabour { numOfThreads = 1 }
        val config =
            sqsRegistryConfiguration("test-registry", localstack.region) {
                endpointOverride = localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = localstack.accessKey
                    secretAccessKey = localstack.secretKey
                }
            }

        val sqsRegistry = sqsRegistry(config)
        val sqsProducerConfiguration =
            SqsDataProductionConfiguration(
                produceData = {
                    SqsProductionData.Single {
                        messageBody = "consuming messages deletes the message from queues"
                        messageGroupId = "group_1"
                    }
                },
                resourceNotFound = { _ -> println("Resource not found") },
            )

        val producer =
            sqsProducer(URL.of(URI.create(nonFifoQueueUrl), null), "test-producer", ::println)

        sqsRegistry.addProducer(producer)
        container.register(sqsRegistry)
        container.start()

        container.produceMessage("test-registry", "test-producer", sqsProducerConfiguration)

        await
            .withPollInterval(Duration.ofMillis(500))
            .timeout(Duration.ofSeconds(5))
            .untilAsserted {
                val receiveMessagesResponse = runBlocking {
                    sqsClient.receiveMessage(
                        ReceiveMessageRequest {
                            queueUrl = nonFifoQueueUrl
                            maxNumberOfMessages = 5
                        }
                    )
                }

                assertEquals(true, receiveMessagesResponse.messages?.isNotEmpty())
                assertEquals(
                    receiveMessagesResponse.messages?.first()?.body,
                    "consuming messages deletes the message from queues",
                )
            }

        val messagesSize =
            sqsClient
                .receiveMessage(
                    ReceiveMessageRequest {
                        maxNumberOfMessages = 10
                        visibilityTimeout = 0
                        queueUrl = nonFifoQueueUrl
                    }
                )
                .messages
                ?.size

        assertEquals(null, messagesSize)

        purgeQueue(nonFifoQueueUrl)
        container.stop()
    }

    @Test
    @Tag("sqs-producer-test")
    fun `produce a message to a fifo queue`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            val sqsProducerConfiguration =
                SqsDataProductionConfiguration(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a fifo test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { _ -> println("Resource not found") },
                )

            val producer =
                sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-test-producer") {
                    fail("Error $it")
                }

            container.register(sqsRegistry.addProducer(producer)).start()

            container.produceMessage(
                "test-registry",
                "fifo-test-producer",
                sqsProducerConfiguration,
            )

            await
                .withPollInterval(Duration.ofSeconds(1))
                .timeout(Duration.ofSeconds(5))
                .untilAsserted {
                    val receiveMessagesResponse = runBlocking {
                        sqsClient.receiveMessage(
                            ReceiveMessageRequest {
                                queueUrl = fifoQueueUrl
                                maxNumberOfMessages = 5
                            }
                        )
                    }

                    assertEquals(true, receiveMessagesResponse.messages?.isNotEmpty())
                    assertEquals(
                        receiveMessagesResponse.messages?.first()?.body,
                        "this is a fifo test message",
                    )
                }
            purgeQueue(fifoQueueUrl)
            container.stop()
        }

    @Test
    @Tag("sqs-producer-test")
    fun `produce a message with wrong registry key triggers resource not found error`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            var resourceNotFound: ProductionResourceNotFound? = null
            val sqsProducerConfiguration =
                SqsDataProductionConfiguration(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a fifo test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { error -> resourceNotFound = error },
                )

            val producer =
                sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-test-producer") {
                    println(it)
                }

            sqsRegistry.addProducer(producer)
            container.register(sqsRegistry)
            container.start()

            container.produceMessage(
                "wrong-registry",
                "fifo-test-producer",
                sqsProducerConfiguration,
            )

            await.withPollDelay(Duration.ofSeconds(1)).untilAsserted {
                assertEquals(RegistryNotFound("wrong-registry"), resourceNotFound)
            }
            container.stop()
        }

    @Test
    @Tag("sqs-producer-test")
    fun `produce a message with wrong producer key triggers resource not found error`() =
        runTest(UnconfinedTestDispatcher()) {
            val container = tabour { numOfThreads = 1 }
            val config =
                sqsRegistryConfiguration("test-registry", localstack.region) {
                    endpointOverride =
                        localstack.getEndpointOverride(LocalStackContainer.Service.SQS)
                    credentialsProvider = StaticCredentialsProvider {
                        accessKeyId = localstack.accessKey
                        secretAccessKey = localstack.secretKey
                    }
                }

            val sqsRegistry = sqsRegistry(config)
            var resourceNotFound: ProductionResourceNotFound? = null
            val sqsProducerConfiguration =
                SqsDataProductionConfiguration(
                    produceData = {
                        SqsProductionData.Single {
                            messageBody = "this is a fifo test message"
                            messageGroupId = "group_1"
                        }
                    },
                    resourceNotFound = { error -> resourceNotFound = error },
                )

            val producer =
                sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-test-producer", ::println)

            sqsRegistry.addProducer(producer)
            container.register(sqsRegistry)
            container.start()

            container.produceMessage("test-registry", "wrong-producer", sqsProducerConfiguration)

            await.withPollDelay(Duration.ofSeconds(1)).untilAsserted {
                assertEquals(ProducerNotFound("wrong-producer"), resourceNotFound)
            }

            container.stop()
        }

    private suspend fun purgeQueue(url: String) {
        sqsClient.purgeQueue(PurgeQueueRequest { queueUrl = url })
    }
}
