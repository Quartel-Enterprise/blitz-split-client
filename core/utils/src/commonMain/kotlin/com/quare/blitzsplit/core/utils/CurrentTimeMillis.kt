package com.quare.blitzsplit.core.utils

// Why: injected so tests control the Last-Write-Wins timestamps of the sync.
fun interface CurrentTimeMillis {
    operator fun invoke(): Long
}
