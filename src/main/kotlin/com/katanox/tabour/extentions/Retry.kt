package com.katanox.tabour.extentions

import kotlinx.coroutines.delay
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

suspend inline fun <T> retry(
    times: Int = 1,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000, // 1 second
    factor: Double = 2.0,
    block: () -> T,
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            logger.warn(e.message)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}
