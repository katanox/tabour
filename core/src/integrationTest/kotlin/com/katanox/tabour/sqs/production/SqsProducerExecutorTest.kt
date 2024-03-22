package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqs.sqsProducer
import java.net.URI
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqsProducerExecutorTest {
    private val localstack =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:3.2"))
            .withServices(LocalStackContainer.Service.SQS)
            .withReuse(true)

    private val credentials = AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey)
    private lateinit var sqsClient: SqsClient
    private lateinit var nonFifoQueueUrl: String
    private lateinit var fifoQueueUrl: String

    @AfterEach
    fun cleanup() {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(nonFifoQueueUrl).build())
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(fifoQueueUrl).build())
    }

    @AfterAll
    fun deleteQueues() {
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(nonFifoQueueUrl).build())
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(fifoQueueUrl).build())
    }

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
    }

    @Test
    fun testProduceToFifoQueue() = runTest {
        val executor = SqsProducerExecutor(sqsClient)

        val producer =
            sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-queue-producer", ::println)
        var producedCount = 0
        val pfc =
            SqsDataProductionConfiguration(
                dataProduced = { _, _ -> producedCount++ },
                produceData = { FifoDataProduction("my message", "groupid") },
                resourceNotFound = { _ -> }
            )

        executor.produce(producer, pfc)

        val response =
            sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(fifoQueueUrl).build())

        assertEquals(1, producedCount)
        assertTrue { response.messages().isNotEmpty() }
    }

    @Test
    fun testProduceToFifoQueueWithDeduplicationId() = runTest {
        val executor = SqsProducerExecutor(sqsClient)

        val producer =
            sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-queue-producer", ::println)
        var producedCount = 0
        val pfc =
            SqsDataProductionConfiguration(
                dataProduced = { _, _ -> producedCount++ },
                produceData = {
                    FifoDataProduction(
                        "my message dedup",
                        "groupid",
                        messageDeduplicationId = "dedup"
                    )
                },
                resourceNotFound = { _ -> }
            )

        val pfc2 =
            SqsDataProductionConfiguration(
                dataProduced = { _, _ -> producedCount++ },
                produceData = {
                    FifoDataProduction(
                        "my message dedup",
                        "groupid",
                        messageDeduplicationId = "dedup"
                    )
                },
                resourceNotFound = { _ -> }
            )

        executor.produce(producer, pfc)
        executor.produce(producer, pfc2)

        val response =
            sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(fifoQueueUrl).build())

        assertEquals(2, producedCount)
        assertEquals(1, response.messages().size)
    }

    @Test
    fun testProduceToNonFifoQueue() = runTest {
        val executor = SqsProducerExecutor(sqsClient)

        val producer =
            sqsProducer(
                URL.of(URI.create(nonFifoQueueUrl), null),
                "non-fifo-queue-producer",
                ::println
            )
        var producedCount = 0
        val pfc =
            SqsDataProductionConfiguration(
                dataProduced = { _, _ -> producedCount++ },
                produceData = { NonFifoDataProduction("my message") },
                resourceNotFound = { _ -> }
            )

        executor.produce(producer, pfc)

        val response =
            sqsClient.receiveMessage(
                ReceiveMessageRequest.builder().queueUrl(nonFifoQueueUrl).build()
            )

        assertEquals(1, producedCount)
        assertTrue { response.messages().isNotEmpty() }
    }

    @Test
    fun produceBatch() = runTest {
        val executor = SqsProducerExecutor(sqsClient)

        val producer =
            sqsProducer(URL.of(URI.create(fifoQueueUrl), null), "fifo-queue-producer", ::println)
        var producedCount = 0
        val pfc =
            SqsDataProductionConfiguration(
                dataProduced = { _, _ -> producedCount++ },
                produceData = {
                    BatchDataForProduction(
                        listOf(
                            FifoDataProduction("batch message", messageGroupId = "ohello"),
                            FifoDataProduction("batch message 2", messageGroupId = "ohello")
                        )
                    )
                },
                resourceNotFound = { _ -> }
            )

        executor.produce(producer, pfc)

        val response =
            sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(fifoQueueUrl)
                    .maxNumberOfMessages(10)
                    .build()
            )

        assertEquals(2, producedCount)
        assertEquals(2, response.messages().size)
    }
}
