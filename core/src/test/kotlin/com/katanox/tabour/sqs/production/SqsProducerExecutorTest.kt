package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqsProducer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

class SqsProducerExecutorTest {
    private val sqs = mockk<SqsAsyncClient>()
    private val executor = SqsProducerExecutor(sqs)

    @Test
    fun `producing with empty url does not fire a request`() = runTest {
        val producer = sqsProducer { produce = mockk() }
        val response = SendMessageResponse.builder().build()

        coEvery { sqs.sendMessage(any<SendMessageRequest>()) }
            .returns(CompletableFuture.completedFuture(response))

        executor.produce(producer)

        coVerify(exactly = 0) { producer.produce() }
    }

    @Test
    fun `producing with valid url does fires a request`() = runTest {
        val producer = sqsProducer {
            queueUrl = URI("http://my-url.com/queue/000000")
            produce = mockk()
        }
        val response = SendMessageResponse.builder().build()
        val request =
            SendMessageRequest.builder()
                .messageBody("body")
                .queueUrl("http://my-url.com/queue/000000")
                .build()

        coEvery { producer.produce() }.returns("body")
        coEvery { sqs.sendMessage(request) }.returns(CompletableFuture.completedFuture(response))

        executor.produce(producer)

        coVerify(exactly = 1) { producer.produce() }
    }
}
