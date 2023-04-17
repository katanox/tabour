package com.katanox.tabour

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SqsPollerKtTest {

    @Test
    fun `test retry with no exceptions runs 1 time`() = runTest {
        var triesCounter = 0
        var errorCounter = 0
        retry(1, { errorCounter++ }) {
            triesCounter++
            Unit
        }

        assertEquals(1, triesCounter)
        assertEquals(0, errorCounter)
    }

    @Test
    fun `test retry with exceptions runs RETRY_TIMES times`() = runTest {
        var errorCounter = 0
        var triesCounter = 0

        retry(5, { errorCounter++ }) {
            triesCounter++
            throw Exception("test retry")
        }

        assertEquals(5, triesCounter)
        assertEquals(1, errorCounter)
    }
}
