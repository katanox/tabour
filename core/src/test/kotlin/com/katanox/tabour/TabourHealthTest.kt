package com.katanox.tabour

import com.katanox.tabour.configuration.core.tabour
import com.katanox.tabour.configuration.sqs.sqsConsumer
import com.katanox.tabour.configuration.sqs.sqsConsumerConfiguration
import com.katanox.tabour.configuration.sqs.sqsRegistry
import com.katanox.tabour.configuration.sqs.sqsRegistryConfiguration
import java.net.URI
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class TabourHealthTest {
    val consumerUrl: URL = URL.of(URI.create("https://localhost"), null)

    @Test
    fun testTabourHealthyWithNoConsumptionStartIsTrue() {
        val t = tabour {}
        assertTrue(t.isHealthy)
    }

    @Test
    fun testTabourHealthyWithConsumptionStartAndNoConsumersIsTrue() {
        val t = tabour {}
        t.start()
        assertTrue(t.isHealthy)
    }

    @Test
    fun testTabourHealthWithConsumerLastPollGreaterThanCeilingIsFalse() {
        val t = tabour {}
        val consumer =
            sqsConsumer(consumerUrl, "", { _ -> true }, { _ -> }) {
                lastPoll = Clock.System.now().minus(15.minutes)
                config = sqsConsumerConfiguration { pollHealthTimeCeiling = 10.minutes }
            }

        val registry = sqsRegistry(sqsRegistryConfiguration("key", "eu-west-1"))
        t.register(registry.addConsumer(consumer))
        t.start()
        assertFalse(t.isHealthy)
    }

    @Test
    fun testTabourHealthWithConsumerLastPollLessThanCeilingIsTrue() {
        val t = tabour {}
        val consumer =
            sqsConsumer(consumerUrl, "", { _ -> true }, { _ -> }) {
                lastPoll = Clock.System.now().minus(5.minutes)
                config = sqsConsumerConfiguration { pollHealthTimeCeiling = 10.minutes }
            }

        val registry = sqsRegistry(sqsRegistryConfiguration("key", "eu-west-1"))
        t.register(registry.addConsumer(consumer))
        t.start()
        assertTrue(t.isHealthy)
    }
}
