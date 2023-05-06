package com.katanox.tabour.sqs.production

interface TabourProducer<T> {
    val key: T
    val onError: (ProducerError<T>) -> Unit
}