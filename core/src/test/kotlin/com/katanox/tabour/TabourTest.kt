package com.katanox.tabour

import com.katanox.tabour.config.Configuration
import config.IntegrationType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TabourTest {

    @Test
    fun addSqsConfiguration() {
        val registry = SqsRegistry()
        val tabour = Tabour()

        val sqs =
            Configuration.sqs {
                url = ""
                successFn = {}
                errorFn = {}
            }

        registry.addSqsConsumer(sqs)

        tabour.register(registry)


        assertEquals(tabour.registries().first().type, IntegrationType.SQS)
    }
}
