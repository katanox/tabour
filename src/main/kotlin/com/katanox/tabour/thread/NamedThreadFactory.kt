package com.katanox.tabour.thread

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(
    private val currentThreadCount: AtomicInteger = AtomicInteger(0),
    private val threadNamePrefix: String
) : ThreadFactory {

    override fun newThread(runnable: Runnable?): Thread {
        val threadNumber = currentThreadCount.incrementAndGet()
        val threadName = String.format("%s-%d", threadNamePrefix, threadNumber)
        return Thread(runnable, threadName)
    }
}
