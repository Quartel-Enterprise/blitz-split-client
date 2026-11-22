package com.quare.blitzsplit.core.sync

// Why: a sync that went through keeps its time on the device, the watch's "Updated at 14:32".
fun interface LastSyncRecorder {
    suspend fun record()
}
