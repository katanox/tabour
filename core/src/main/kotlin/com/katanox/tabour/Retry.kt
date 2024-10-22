package com.katanox.tabour

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
            tries++

            if (tries == repeatTimes) {
                onError(e)
            }
        }
    }
}
