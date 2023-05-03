package com.katanox.tabour.sqs.production

data class ProducerError(val message: String, val producerKey: String)