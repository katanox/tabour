package com.katanox.tabour.consumer

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchResponse
import aws.sdk.kotlin.services.sqs.model.Message
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import com.katanox.tabour.configuration.sqs.sqsConsumer
import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.consumption.SqsPoller
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.net.URI
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException

@ExperimentalCoroutinesApi
class SqsPollerTest {
    @Test
    fun `test accept with one worker for one message runs once the successFn`() =
        runTest(UnconfinedTestDispatcher()) {
            val successFunc: suspend (Message) -> Boolean = mockk()
            val errorFunc: (ConsumptionError) -> Unit = mockk()

            every { errorFunc(any()) }.returns(Unit)

            val sqs: SqsClient = mockk()
            val sqsPoller = SqsPoller(sqs)
            var counter = 0
            val configuration =
                spyk(
                    sqsConsumer(
                        URL.of(URI.create("https://katanox.com"), null),
                        "my-key",
                        onSuccess = {
                            counter++
                            true
                        },
                        onError = errorFunc,
                    ) {
                        config = sqsConsumerConfiguration {
                            concurrency = 1
                            consumeWhile = { counter < 1 }
                        }
                    }
                )
            val request = ReceiveMessageRequest {
                queueUrl = "https://katanox.com"
                maxNumberOfMessages = 10
                waitTimeSeconds = 0
            }

            val message = Message {
                body = "body"
                receiptHandle = "1"
                messageId = "12345"
            }

            val response: ReceiveMessageResponse = ReceiveMessageResponse {
                messages = listOf(message)
            }

            coEvery { sqs.receiveMessage(request) }.returns(response)
            coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
                .returns(mockk<DeleteMessageBatchResponse>())
            coEvery { successFunc(message) }.returns(true)

            launch { sqsPoller.poll(listOf(configuration)) }

            sqsPoller.stopPolling()

            assertEquals(1, counter)
        }

    @Test
    fun `test accept with 5 workers for one message runs 5 times the successFn`() =
        runTest(UnconfinedTestDispatcher()) {
            val successFunc: suspend (Message) -> Boolean = mockk()
            val errorFunc: (ConsumptionError) -> Unit = mockk()

            every { errorFunc(any()) }.returns(Unit)

            val sqs: SqsClient = mockk()
            val sqsPoller = SqsPoller(sqs)
            var counter = 0
            val configuration =
                spyk(
                    sqsConsumer(
                        URL.of(URI.create("https://katanox.com"), null),
                        "my-key",
                        onSuccess = {
                            counter++
                            true
                        },
                        onError = errorFunc,
                    ) {
                        config = sqsConsumerConfiguration {
                            concurrency = 5
                            consumeWhile = { counter < 5 }
                        }
                    }
                )
            val request = ReceiveMessageRequest {
                queueUrl = "https://katanox.com"
                maxNumberOfMessages = 10
                waitTimeSeconds = 0
            }

            val message = Message {
                body = "body"
                receiptHandle = "1"
                messageId = "12345"
            }

            val response: ReceiveMessageResponse = ReceiveMessageResponse {
                messages = listOf(message)
            }

            coEvery { sqs.receiveMessage(request) }.returns(response)
            coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
                .returns(mockk<DeleteMessageBatchResponse>())
            coEvery { successFunc(message) }.returns(true)

            launch { sqsPoller.poll(listOf(configuration)) }

            sqsPoller.stopPolling()

            assertEquals(5, counter)
        }

    @Test
    fun `test accept an exception calls error fn`() =
        runTest(UnconfinedTestDispatcher()) {
            val successFunc: suspend (Message) -> Boolean = mockk()
            val errorFunc: (ConsumptionError) -> Unit = mockk()
            val sqs: SqsClient = mockk()
            val sqsPoller = SqsPoller(sqs)
            var counter = 0
            val configuration =
                spyk(
                    sqsConsumer(
                        URL.of(URI.create("https://katanox.com"), null),
                        "my-key",
                        onSuccess = successFunc,
                        onError = errorFunc,
                    ) {
                        config = sqsConsumerConfiguration {
                            concurrency = 1
                            consumeWhile = {
                                counter++
                                counter < 2
                            }
                        }
                    }
                )
            val request = ReceiveMessageRequest {
                queueUrl = "https://katanox.com"
                maxNumberOfMessages = 10
                waitTimeSeconds = 0
            }

            val exception =
                AwsServiceException.builder()
                    .awsErrorDetails(AwsErrorDetails.builder().build())
                    .build()

            coEvery { errorFunc(any()) }.returns(Unit)
            coEvery { sqs.receiveMessage(request) }.throws(exception)

            launch { sqsPoller.poll(listOf(configuration)) }

            sqsPoller.stopPolling()

            coVerify(exactly = 0) { successFunc(any()) }
            verify(exactly = 1) {
                errorFunc(ConsumptionError.AwsError(AwsErrorDetails.builder().build()))
            }
        }

    @Test
    fun `when onSuccess throws an exception, onError with the throwable is called`() =
        runTest(UnconfinedTestDispatcher()) {
            var expectedError: ConsumptionError? = null
            val successFunc: suspend (Message) -> Boolean = { throw Exception("Random") }
            val errorFunc: (ConsumptionError) -> Unit = { expectedError = it }
            val sqs: SqsClient = mockk()
            val sqsPoller = SqsPoller(sqs)
            var counter = 0
            val configuration =
                spyk(
                    sqsConsumer(
                        URL.of(URI.create("https://katanox.com"), null),
                        "my-key",
                        onSuccess = successFunc,
                        onError = errorFunc,
                    ) {
                        config = sqsConsumerConfiguration {
                            concurrency = 1
                            consumeWhile = {
                                counter++
                                counter < 2
                            }
                        }
                    }
                )
            val request = ReceiveMessageRequest {
                queueUrl = "https://katanox.com"
                maxNumberOfMessages = 10
                waitTimeSeconds = 0
            }

            val message = Message {
                body = "body"
                receiptHandle = "1"
                messageId = "12345"
            }

            val response = ReceiveMessageResponse { messages = listOf(message) }

            coEvery { sqs.receiveMessage(request) }.returns(response)
            coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
                .returns(mockk<DeleteMessageBatchResponse>())

            launch { sqsPoller.poll(listOf(configuration)) }

            sqsPoller.stopPolling()

            assertIs<ConsumptionError.ThrowableDuringHanding>(expectedError)
        }
}
