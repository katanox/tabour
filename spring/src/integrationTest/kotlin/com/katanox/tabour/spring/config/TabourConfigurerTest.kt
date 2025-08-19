package com.katanox.tabour.spring.config

import com.katanox.tabour.configuration.core.tabour
import com.katanox.tabour.configuration.sqs.sqsRegistry
import com.katanox.tabour.configuration.sqs.sqsRegistryConfiguration
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region

@ExperimentalCoroutinesApi
class TabourConfigurerTest {
    @Test
    fun `test constructTabourContainer without annotation does not start tabour`() {
        val tabourContainer = tabour { numOfThreads = 1 }

        launchTabour(ClassWithoutAnnotation::class.java, tabourContainer) { emptyList() }

        assertFalse(tabourContainer.running())
    }

    @Test
    fun `test constructTabourContainer with annotation but no registries does not start the container`() =
        runTest {
            val tabourContainer = tabour { numOfThreads = 1 }

            launchTabour(ClassWithoutAnnotation::class.java, tabourContainer) { emptyList() }

            assertFalse(tabourContainer.running())
        }

    @Test
    fun `test constructTabourContainer with annotation and registries starts the container`() =
        runTest(UnconfinedTestDispatcher()) {
            val tabourContainer = tabour { numOfThreads = 1 }

            launchTabour(ClassWithAnnotation::class.java, tabourContainer) {
                listOf(sqsRegistry(sqsRegistryConfiguration("", Region.US_EAST_1)))
            }

            assertTrue(tabourContainer.running())
            tabourContainer.stop()
        }

    @Test
    fun `test shutdown hook`() =
        runTest(UnconfinedTestDispatcher()) {
            val tabourContainer = tabour { numOfThreads = 1 }
            val disposer = TabourDisposer(tabourContainer)

            tabourContainer.start()

            advanceUntilIdle()

            assertTrue(tabourContainer.running())

            disposer.destroy()

            assertFalse(tabourContainer.running())
        }

    private class ClassWithoutAnnotation

    @AutoconfigureTabour private class ClassWithAnnotation
}
