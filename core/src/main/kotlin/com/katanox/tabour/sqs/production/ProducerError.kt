package com.katanox.tabour.sqs.production

data class ProducerError<T>(val message: String, val producerKey: T)