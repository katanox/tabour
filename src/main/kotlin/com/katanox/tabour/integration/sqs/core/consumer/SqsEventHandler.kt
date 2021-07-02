package com.katanox.tabour.integration.sqs.core.consumer

import com.katanox.tabour.base.IEventConsumerBase
import com.katanox.tabour.config.TabourAutoConfigs
import com.katanox.tabour.extentions.ConsumerAction
import io.github.resilience4j.retry.event.RetryOnErrorEvent
import mu.KotlinLogging
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, scopeName = "prototype")
class SqsEventHandler(
    val sqsQueueUrl: String = "",
    val consumerAction: ConsumerAction = {},
    val tabourConfigs: TabourAutoConfigs
) : IEventConsumerBase {
    /**
     * Called just before the handle() method is being called. You can implement this method to
     * initialize the thread handling the message with [ThreadLocal]s or add an MDC context for
     * logging or something similar. Just make sure that you clean up after yourself in the
     * onAfterHandle() method.
     *
     *
     * The default implementation does nothing.
     */
    fun onBeforeHandle(message: String) {
    }

    /**
     * Called after a message has been handled, irrespective of the success. In case of an exception
     * during the invocation of handle(), onAfterHandle() is called AFTER the exception has been
     * handled by an [ExceptionHandler] so that the exception handler still has any context that
     * might have been set in onBeforeHandle().
     *
     *
     * The default implementation does nothing.
     */
    fun onAfterHandle(message: String) {}

    fun handle(message: String) {
        val retry = tabourConfigs.retryRegistry().retry("handler")
        retry
            .eventPublisher
            .onError { event: RetryOnErrorEvent? ->
                logger.warn(
                    "error {} handling message {}",
                    event,
                    message
                )
            }
        retry.executeRunnable { consumerAction(message) }
    }


}
