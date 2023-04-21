package com.katanox.tabour.consumer

import com.katanox.tabour.configuration.sqsConsumer
import com.katanox.tabour.configuration.sqsConsumerConfiguration
import com.katanox.tabour.consumption.ConsumptionError
import com.katanox.tabour.sqs.consumption.SqsPoller
import io.mockk.*
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class SqsPollerTest {
    val successFunc: (Message) -> Unit = mockk()
    val errorFunc: (ConsumptionError) -> Unit = mockk()

    @Test
    fun `test accept with one worker for one message runs once the successFn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val sqsPoller = SqsPoller(sqs)
        val configuration =
            spyk(
                sqsConsumer {
                    queueUrl = URI("url")
                    onSuccess = successFunc
                    onError = errorFunc
                    config = sqsConsumerConfiguration { concurrency = 1 }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl(configuration.queueUrl.toASCIIString())
                .maxNumberOfMessages(1)
                .waitTimeSeconds(10)
                .build()

        val message: Message =
            Message.builder().body("body").receiptHandle("1").messageId("12345").build()

        val response: ReceiveMessageResponse =
            ReceiveMessageResponse.builder().messages(message).build()

        coEvery { sqs.receiveMessage(request) }.returns(CompletableFuture.completedFuture(response))
        coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
            .returns(CompletableFuture.completedFuture(mockk<DeleteMessageBatchResponse>()))
        every { successFunc(message) }.returns(Unit)

        sqsPoller.accept(configuration)

        verify(exactly = 1) { successFunc(message) }
        verify(exactly = 0) { errorFunc(any()) }
    }

    @Test
    fun `test accept with 5 workers for one message runs twice the successFn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val sqsPoller = SqsPoller(sqs)
        val configuration =
            spyk(
                sqsConsumer {
                    queueUrl = URI("url")
                    onSuccess = successFunc
                    onError = errorFunc
                    config = sqsConsumerConfiguration { concurrency = 5 }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl(configuration.queueUrl.toASCIIString())
                .waitTimeSeconds(10)
                .maxNumberOfMessages(1)
                .build()

        val message: Message =
            Message.builder().body("body").receiptHandle("1").messageId("12345").build()

        val response: ReceiveMessageResponse =
            ReceiveMessageResponse.builder().messages(message).build()

        coEvery { sqs.receiveMessage(request) }.returns(CompletableFuture.completedFuture(response))
        coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
            .returns(CompletableFuture.completedFuture(mockk<DeleteMessageBatchResponse>()))
        every { successFunc(message) }.returns(Unit)

        sqsPoller.accept(configuration)

        verify(exactly = 5) { successFunc(message) }
        verify(exactly = 0) { errorFunc(any()) }
    }

    @Test
    fun `test accept an exception calls error fn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val sqsPoller = SqsPoller(sqs)
        val configuration =
            spyk(
                sqsConsumer {
                    queueUrl = URI("url")
                    onSuccess = successFunc
                    onError = errorFunc
                    config = sqsConsumerConfiguration { concurrency = 1 }
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl(configuration.queueUrl.toASCIIString())
                .maxNumberOfMessages(1)
                .waitTimeSeconds(10)
                .build()

        val exception =
            AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder().build()).build()

        coEvery { errorFunc(any()) }.returns(Unit)
        coEvery { sqs.receiveMessage(request) }.throws(exception)

        sqsPoller.accept(configuration)

        verify(exactly = 0) { successFunc(any()) }
        verify(exactly = 1) {
            errorFunc(ConsumptionError.AwsError(AwsErrorDetails.builder().build()))
        }
    }
}
