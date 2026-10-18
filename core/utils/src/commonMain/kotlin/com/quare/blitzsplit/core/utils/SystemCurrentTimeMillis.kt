package com.quare.blitzsplit.core.utils

import kotlin.time.Clock

class SystemCurrentTimeMillis : CurrentTimeMillis {
    override fun invoke(): Long = Clock.System.now().toEpochMilliseconds()
}
