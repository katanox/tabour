package com.katanox.tabour.sqs.production

interface TabourProducer {
    val key: String
    val onError: (ProducerError) -> Unit
}