package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.base.IEventHandlerBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.extentions.ConsumerAction
import com.katanox.tabour.extentions.FailureAction
import com.katanox.tabour.extentions.retry
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, scopeName = "prototype")
class SqsEventHandler(
    val sqsQueueUrl: String = "",
    val consumerAction: ConsumerAction = {},
    val tabourConfigs: TabourAutoConfigs,
    val failureAction: FailureAction,
) : IEventHandlerBase {
    /**
     * Called just before the handle() method is being called. You can implement this method to
     * initialize the thread handling the message with [ThreadLocal] s or add an MDC context for
     * logging or something similar. Just make sure that you clean up after yourself in the
     * onAfterHandle() method.
     *
     * The default implementation does nothing.
     */
    fun onBeforeHandle(message: String) {}

    /**
     * Called after a message has been handled, irrespective of the success. In case of an exception
     * during the invocation of handle(), onAfterHandle() is called AFTER the exception would be
     * logged and the message will be retried, in case of an exception keep being thrown,
     * the message would be discarded from being deleted
     * have been set in onBeforeHandle().
     *
     * The default implementation does nothing.
     */
    fun onAfterHandle(message: String) {}

    fun handle(message: String) {
        runBlocking {
            retry(times = tabourConfigs.tabourProperties.maxRetryCount) {
                consumerAction(message)
            }
        }
    }
}
