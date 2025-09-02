package com.katanox.tabour.sqs.production

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.CreateQueueRequest
import aws.sdk.kotlin.services.sqs.model.DeleteQueueRequest
import aws.sdk.kotlin.services.sqs.model.PurgeQueueRequest
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequestEntry
import aws.smithy.kotlin.runtime.net.url.Url
import com.katanox.tabour.configuration.sqs.sqsProducer
import java.net.URI
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(DelicateCoroutinesApi::class)
class SqsProducerExecutorTest {
    private val localstack =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:4.7"))
            .withServices(LocalStackContainer.Service.SQS)
            .withReuse(true)

    private suspend fun sqsClient(): SqsClient =
        SqsClient.fromEnvironment {
            region = localstack.region

            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = localstack.accessKey
                secretAccessKey = localstack.secretKey
            }

            endpointUrl =
                Url.parse(
                    localstack
                        .getEndpointOverride(LocalStackContainer.Service.SQS)
                        .toURL()
                        .toString()
                )
        }

    private lateinit var nonFifoQueueUrl: String
    private lateinit var fifoQueueUrl: String

    @AfterEach
    fun cleanup() {
        runBlocking {
            sqsClient().let {
                it.purgeQueue(PurgeQueueRequest { queueUrl = nonFifoQueueUrl })
                it.purgeQueue(PurgeQueueRequest { queueUrl = fifoQueueUrl })
            }
        }
    }

    @AfterAll
    fun deleteQueues() {
        runBlocking {
            sqsClient().let {
                it.deleteQueue(DeleteQueueRequest { queueUrl = nonFifoQueueUrl })
                it.deleteQueue(DeleteQueueRequest { queueUrl = fifoQueueUrl })
            }
        }
    }

    @BeforeAll
    fun setup() {
        runBlocking {
            localstack.start()
            val sqsClient = sqsClient()

            nonFifoQueueUrl =
                sqsClient.createQueue(CreateQueueRequest { queueName = "my-queue" }).queueUrl
                    ?: fail("Queue not created")

            fifoQueueUrl =
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

    @Test
    fun testProduceToFifoQueue() = runTest {
        val sqsClient = sqsClient()
        val executor = SqsProducerExecutor()

        val producer =
            sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-queue-producer") {
                fail(it.toString())
            }
        val pfc =
            SqsDataProductionConfiguration(
                produceData = {
                    SqsProductionData.Single {
                        messageBody = "this is a fifo test message"
                        messageGroupId = "group_1"
                    }
                },
                resourceNotFound = { _ -> },
            )

        sqsClient.use { client ->
            executor.produce(client, producer, pfc)

            val response =
                sqsClient.receiveMessage(ReceiveMessageRequest { queueUrl = fifoQueueUrl })

            assertEquals(response.messages?.isNotEmpty(), true)
        }
    }

    @Test
    fun testProduceToFifoQueueWithDeduplicationId() = runTest {
        val executor = SqsProducerExecutor()

        val producer =
            sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-queue-producer", ::println)
        val pfc =
            SqsDataProductionConfiguration(
                produceData = {
                    SqsProductionData.Single {
                        messageBody = "this is a fifo test message"
                        messageGroupId = "group_1"
                        messageDeduplicationId = "dedup"
                    }
                },
                resourceNotFound = { _ -> },
            )

        val pfc2 =
            SqsDataProductionConfiguration(
                produceData = {
                    SqsProductionData.Single {
                        messageBody = "this is a fifo test message"
                        messageGroupId = "group_1"
                        messageDeduplicationId = "dedup"
                    }
                },
                resourceNotFound = { _ -> },
            )

        sqsClient().use { client ->
            executor.produce(client, producer, pfc)
            executor.produce(client, producer, pfc2)

            val response = client.receiveMessage(ReceiveMessageRequest { queueUrl = fifoQueueUrl })

            assertEquals(1, response.messages?.size)
        }
    }

    @Test
    fun testProduceToNonFifoQueue() = runTest {
        val executor = SqsProducerExecutor()

        val producer =
            sqsProducer(
                URL.of(URI.create(nonFifoQueueUrl), null),
                "non-fifo-queue-producer",
                ::println,
            )
        val pfc =
            SqsDataProductionConfiguration(
                produceData = {
                    SqsProductionData.Single {
                        messageBody = "my message"
                        messageGroupId = "group_1"
                    }
                },
                resourceNotFound = { _ -> },
            )

        sqsClient().use { client ->
            executor.produce(client, producer, pfc)

            val response =
                client.receiveMessage(ReceiveMessageRequest { queueUrl = nonFifoQueueUrl })

            assertEquals(response.messages?.isNotEmpty(), true)
        }
    }

    @Test
    fun produceBatch() = runTest {
        val executor = SqsProducerExecutor()

        val producer =
            sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-queue-producer", ::println)
        val pfc =
            SqsDataProductionConfiguration(
                produceData = {
                    SqsProductionData.Batch {
                        entries =
                            listOf(
                                SendMessageBatchRequestEntry {
                                    id = "1"
                                    messageBody = "message 1"
                                    messageGroupId = "group_1"
                                },
                                SendMessageBatchRequestEntry {
                                    id = "2"
                                    messageBody = "message 2"
                                    messageGroupId = "group_1"
                                },
                            )
                    }
                },
                resourceNotFound = { _ -> },
            )

        sqsClient().use { client ->
            executor.produce(client, producer, pfc)

            val response =
                client.receiveMessage(
                    ReceiveMessageRequest {
                        queueUrl = fifoQueueUrl
                        maxNumberOfMessages = 10
                    }
                )

            assertEquals(2, response.messages?.size)
        }
    }
}
