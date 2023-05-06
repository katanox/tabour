package com.katanox.tabour.sqs.production

interface TabourProducer<K> {
    val key: K
    val onError: (ProducerError<K>) -> Unit
}