package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqs.sqsProducer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class SqsProducerTest {

    @Test
    fun `urlWasSet is false by default`() {
        val producer = sqsProducer {}
        assertFalse(producer.urlWasSet())
    }

    @Test
    fun `urlWasSet is true if we assign a url`() {
        val producer = sqsProducer {
            queueUrl = URI("https://test.com")
        }
        assertTrue(producer.urlWasSet())
    }
}
