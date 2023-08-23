package com.katanox.tabour.spring.config

import com.katanox.tabour.configuration.sqs.sqsRegistry
import com.katanox.tabour.configuration.sqs.sqsRegistryConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region

@ExperimentalCoroutinesApi
@SpringBootTest(classes = [TabourConfigurer::class])
@ExtendWith(SpringExtension::class)
class TabourConfigurerTest(private val context: ApplicationContext) {
    @Test
    fun `test constructTabourContainer without annotation does not start tabour`() {
        val tabourContainer =
            constructTabourContainer(ClassWithoutAnnotation::class.java, 1) { emptyList() }

        assertFalse(tabourContainer.running())
        assertEquals(tabourContainer.config.numOfThreads, 1)
    }

    @Test
    fun `test constructTabourContainer without annotation does not start tabour but assigns num of threads`() {
        val tabourContainer =
            constructTabourContainer(ClassWithoutAnnotation::class.java, 5) { emptyList() }

        assertFalse(tabourContainer.running())
        assertEquals(tabourContainer.config.numOfThreads, 5)
    }

    @Test
    fun `test constructTabourContainer with annotation but no registries does not start the container`() =
        runTest {
            val tabourContainer =
                constructTabourContainer(ClassWithAnnotation::class.java, 2) { emptyList() }

            assertFalse(tabourContainer.running())
        }

    @Test
    fun `test constructTabourContainer with annotation and registries starts the container`() =
        runTest(UnconfinedTestDispatcher()) {
            val tabourContainer =
                constructTabourContainer(ClassWithAnnotation::class.java, 2) {
                    listOf(
                        sqsRegistry(
                            sqsRegistryConfiguration(
                                "",
                                EnvironmentVariableCredentialsProvider.create(),
                                Region.US_EAST_1
                            )
                        )
                    )
                }

            advanceUntilIdle()

            assertTrue(tabourContainer.running())
            tabourContainer.stop()
        }

    private class ClassWithoutAnnotation
    @AutoconfigureTabour private class ClassWithAnnotation
}
