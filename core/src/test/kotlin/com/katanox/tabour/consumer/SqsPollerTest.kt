package com.katanox.tabour.consumer

import com.katanox.tabour.configuration.sqs.sqsConsumer
import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.configuration.sqs.sqsPipeline
import com.katanox.tabour.configuration.sqs.sqsProducer
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.consumption.SqsPoller
import com.katanox.tabour.sqs.production.SqsDataForProduction
import com.katanox.tabour.sqs.production.SqsProducerExecutor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.net.URL
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse

class SqsPollerTest {
    val successFunc: suspend (Message) -> Boolean = mockk()
    val errorFunc: (ConsumptionError) -> Unit = mockk()

    @Test
    fun `test accept with one worker for one message runs once the successFn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val executor = SqsProducerExecutor(sqs)
        val sqsPoller = SqsPoller(sqs, executor)
        var counter = 0
        val configuration =
            spyk(
                sqsConsumer(URL("https://katanox.com")) {
                    onSuccess = successFunc
                    onError = errorFunc
                    config = sqsConsumerConfiguration {
                        concurrency = 1
                        consumeWhile = {
                            counter++
                            counter < 2
                        }
                    }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl("https://katanox.com")
                .maxNumberOfMessages(10)
                .waitTimeSeconds(0)
                .build()

        val message: Message =
            Message.builder().body("body").receiptHandle("1").messageId("12345").build()

        val response: ReceiveMessageResponse =
            ReceiveMessageResponse.builder().messages(message).build()

        coEvery { sqs.receiveMessage(request) }.returns(CompletableFuture.completedFuture(response))
        coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
            .returns(CompletableFuture.completedFuture(mockk<DeleteMessageBatchResponse>()))
        coEvery { successFunc(message) }.returns(true)

        sqsPoller.poll(listOf(configuration))

        coVerify(exactly = 1) { successFunc(message) }
        verify(exactly = 0) { errorFunc(any()) }
    }

    @Test
    fun `test accept with pipeline`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val executor = SqsProducerExecutor(sqs)
        val sqsPoller = SqsPoller(sqs, executor)
        var counter = 0
        val transformer = mockk<(Message) -> SqsDataForProduction>()
        val pipelineProducer = sqsProducer(URL("https://katanox.com"), "prod-key")

        val configuration =
            spyk(
                sqsConsumer(URL("https://katanox.com")) {
                    pipeline = sqsPipeline {
                        this.transformer = transformer
                        this.producer = pipelineProducer
                    }

                    onError = errorFunc
                    config = sqsConsumerConfiguration {
                        concurrency = 1
                        consumeWhile = {
                            counter++
                            counter < 2
                        }
                    }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl("https://katanox.com")
                .maxNumberOfMessages(10)
                .waitTimeSeconds(0)
                .build()

        val message: Message =
            Message.builder().body("body").receiptHandle("1").messageId("12345").build()

        val response: ReceiveMessageResponse =
            ReceiveMessageResponse.builder().messages(message).build()

        coEvery { sqs.receiveMessage(request) }.returns(CompletableFuture.completedFuture(response))
        coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
            .returns(CompletableFuture.completedFuture(mockk<DeleteMessageBatchResponse>()))
        every { transformer(message) }.returns(SqsDataForProduction("value", "groupid"))

        sqsPoller.poll(listOf(configuration))

        verify(exactly = 1) { transformer(message) }
        verify(exactly = 0) { errorFunc(any()) }
    }

    @Test
    fun `test accept with pipeline which returns null as body does not consider the message consumed`() =
        runTest {
            val sqs: SqsAsyncClient = mockk()
            val executor = SqsProducerExecutor(sqs)
            val sqsPoller = SqsPoller(sqs, executor)
            val transformer: (Message) -> SqsDataForProduction = {
                SqsDataForProduction(message = null, messageGroupId = "")
            }
            var counter = 0
            val pipelineProducer = sqsProducer(URL("https://katanox.com"), "prod-key")

            val configuration =
                spyk(
                    sqsConsumer(URL("https://katanox.com")) {
                        pipeline = sqsPipeline {
                            this.transformer = transformer
                            this.producer = pipelineProducer
                        }

                        onError = errorFunc
                        config = sqsConsumerConfiguration {
                            concurrency = 1
                            retries = 1
                            consumeWhile = {
                                counter++
                                counter < 2
                            }
                        }
                    }
                )
            val request: ReceiveMessageRequest =
                ReceiveMessageRequest.builder()
                    .queueUrl("https://katanox.com")
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(0)
                    .build()

            val message: Message =
                Message.builder().body("body").receiptHandle("1").messageId("12345").build()

            val response: ReceiveMessageResponse =
                ReceiveMessageResponse.builder().messages(message).build()

            coEvery { sqs.receiveMessage(request) }
                .returns(CompletableFuture.completedFuture(response))
            coEvery { errorFunc(ConsumptionError.UnsuccessfulConsumption(message)) }.returns(Unit)

            sqsPoller.poll(listOf(configuration))

            verify(exactly = 1) { errorFunc(ConsumptionError.UnsuccessfulConsumption(message)) }
        }
    @Test
    fun `test accept with 5 workers for one message runs twice the successFn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val executor = SqsProducerExecutor(sqs)
        val sqsPoller = SqsPoller(sqs, executor)
        var counter = 0
        val configuration =
            spyk(
                sqsConsumer(URL("https://katanox.com")) {
                    onSuccess = successFunc
                    onError = errorFunc
                    config = sqsConsumerConfiguration {
                        concurrency = 5
                        consumeWhile = {
                            counter++
                            counter < 2
                        }
                    }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl("https://katanox.com")
                .maxNumberOfMessages(10)
                .waitTimeSeconds(0)
                .build()

        val message: Message =
            Message.builder().body("body").receiptHandle("1").messageId("12345").build()

        val response: ReceiveMessageResponse =
            ReceiveMessageResponse.builder().messages(message).build()

        coEvery { sqs.receiveMessage(request) }.returns(CompletableFuture.completedFuture(response))
        coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
            .returns(CompletableFuture.completedFuture(mockk<DeleteMessageBatchResponse>()))
        coEvery { successFunc(message) }.returns(true)

        sqsPoller.poll(listOf(configuration))

        coVerify(exactly = 5) { successFunc(message) }
        verify(exactly = 0) { errorFunc(any()) }
    }

    @Test
    fun `test accept an exception calls error fn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val executor = SqsProducerExecutor(sqs)
        val sqsPoller = SqsPoller(sqs, executor)
        var counter = 0
        val configuration =
            spyk(
                sqsConsumer(URL("https://katanox.com")) {
                    onSuccess = successFunc
                    onError = errorFunc
                    config = sqsConsumerConfiguration {
                        concurrency = 1
                        consumeWhile = {
                            counter++
                            counter < 2
                        }
                    }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl("https://katanox.com")
                .maxNumberOfMessages(10)
                .waitTimeSeconds(0)
                .build()

        val exception =
            AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder().build()).build()

        coEvery { errorFunc(any()) }.returns(Unit)
        coEvery { sqs.receiveMessage(request) }.throws(exception)

        sqsPoller.poll(listOf(configuration))

        coVerify(exactly = 0) { successFunc(any()) }
        verify(exactly = 1) {
            errorFunc(ConsumptionError.AwsError(AwsErrorDetails.builder().build()))
        }
    }
}
