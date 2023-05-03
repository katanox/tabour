package com.katanox.tabour.sqs.production

import com.katanox.tabour.configuration.sqsProducer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqsProducerTest {

    @Test
    fun `shouldProduce without changing default produce function should be false`() {
        val producer = sqsProducer {}
        assertFalse(producer.shouldProduce())
    }

    @Test
    fun `shouldProduce with changing default produce function should be true`() {
        val producer = sqsProducer { produce = { "test" } }
        assertTrue(producer.shouldProduce())
    }
}
