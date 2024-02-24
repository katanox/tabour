package com.katanox.tabour.sqs.production

import com.katanox.tabour.plug.ProducerPlug

interface TabourProducer<K> {
    val key: K
    val onError: suspend (ProductionError) -> Unit
    val plugs: List<ProducerPlug>
}
