package com.katanox.tabour

import kotlin.coroutines.cancellation.CancellationException

internal suspend inline fun retry(
    repeatTimes: Int,
    onError: (Throwable) -> Unit,
    crossinline f: suspend () -> Unit,
) {
    var tries = 0

    while (tries < repeatTimes) {
        try {
            f()
            break
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            tries++

            if (tries == repeatTimes) {
                onError(e)
            }
        }
    }
}
