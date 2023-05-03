package com.katanox.tabour.sqs.config

import com.katanox.tabour.configuration.sqs.sqsPipeline
import com.katanox.tabour.configuration.sqs.sqsProducer
import java.net.URI
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqsPipelineTest {
    @Test
    fun `transformer was set is false by default`() {
        val pipeline = sqsPipeline {}

        assertFalse(pipeline.transformerWasSet)
    }

    @Test
    fun `producer was set is false by default with default producer`() {
        val pipeline = sqsPipeline { producer = sqsProducer {} }

        assertFalse(pipeline.producerWasSet)
    }

    @Test
    fun `transformer was set is true if we change the transformer`() {
        val pipeline = sqsPipeline { transformer = { it.body() } }

        assertTrue(pipeline.transformerWasSet)
    }

    @Test
    fun `producer was set is true if producer is configured`() {
        val pipeline = sqsPipeline { producer = sqsProducer { queueUrl = URI("https://url.com") } }

        assertTrue(pipeline.producerWasSet)
    }
}
