package com.katanox.tabour.sqs.production

import java.time.Instant

data class SqsMessageProduced(val messageGroupId: String, val timestamp: Instant)
