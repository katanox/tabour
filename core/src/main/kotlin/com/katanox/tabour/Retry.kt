package com.katanox.tabour

suspend inline fun retry(
    repeatTimes: Int,
    onError: (Exception) -> Unit,
    crossinline f: suspend () -> Unit
) {
  var tries = 0

  while (tries < repeatTimes) {
    try {
      f()
      break
    } catch (e: Exception) {
      tries++

      if (tries == repeatTimes) {
        onError(e)
      }
    }
  }
}
