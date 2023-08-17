package com.katanox.tabour.sqs.production

interface TabourProducer<K> {
  val key: K
  var onError: (ProducerError<K>) -> Unit
}
