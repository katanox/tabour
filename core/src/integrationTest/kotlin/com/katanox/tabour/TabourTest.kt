package com.katanox.tabour

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

@ExperimentalCoroutinesApi
class TabourTest {
    private val localstackImage = DockerImageName.parse("localstack/localstack:0.11.3")
    private val localstack =
        LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.SQS)
            .withReuse(true)

    @Test fun `consume a message`() = runTest { println(localstack) }
}
