package com.katanox.tabour.base

import com.katanox.tabour.extentions.ConsumeAction

interface IEventConsumerBase {
    fun setBusUrl(busUrl: String)
    fun setAction(action: ConsumeAction)
}