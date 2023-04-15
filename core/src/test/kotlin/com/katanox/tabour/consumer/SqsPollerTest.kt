package com.katanox.tabour.consumer

import com.katanox.tabour.config.Configuration.sqs
import io.mockk.*
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse

@OptIn(ExperimentalCoroutinesApi::class)
class SqsPollerTest {
    val successFunc: (Message) -> Unit = mockk()
    val errorFunc: (Exception) -> Unit = mockk()

    @Test
    fun `test accept with one worker for one message runs once the successFn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val sqsPoller = SqsPoller(sqs, StandardTestDispatcher(testScheduler))
        val configuration =
            spyk(
                sqs {
                    workers = 1
                    queueUrl = "url"
                    successFn = successFunc
                    errorFn = errorFunc
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl(configuration.queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(30)
                .build()

        val message: Message =
            Message.builder().body("body").receiptHandle("1").messageId("12345").build()

        val response: ReceiveMessageResponse =
            ReceiveMessageResponse.builder().messages(message).build()

        coEvery { sqs.receiveMessage(request) }.returns(CompletableFuture.completedFuture(response))
        coEvery { sqs.deleteMessageBatch(any<DeleteMessageBatchRequest>()) }
            .returns(CompletableFuture.completedFuture(mockk<DeleteMessageBatchResponse>()))
        every { successFunc(any()) }.returns(Unit)

        sqsPoller.accept(configuration)
        advanceUntilIdle()

        verify(exactly = 1) { successFunc(message) }
        verify(exactly = 0) { errorFunc(any()) }
    }

    @Test
    fun `test accept an exception calls error fn`() = runTest {
        val sqs: SqsAsyncClient = mockk()
        val sqsPoller = SqsPoller(sqs, StandardTestDispatcher(testScheduler))
        val configuration =
            spyk(
                sqs {
                    workers = 1
                    queueUrl = "url"
                    successFn = successFunc
                    errorFn = errorFunc
                }
            )
        val request: ReceiveMessageRequest =
            ReceiveMessageRequest.builder()
                .queueUrl(configuration.queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(30)
                .build()

        coEvery { sqs.receiveMessage(request) }.throws(Exception(""))

        sqsPoller.accept(configuration)
        advanceUntilIdle()

        verify(exactly = 0) { successFunc(any()) }
        verify(exactly = 1) { errorFunc(any()) }
    }
}
