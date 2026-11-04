package com.quare.blitzsplit.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Why: real time follows the other devices' changes only while the app is in front (see SyncCoordinator), which each
 * platform says as it comes and goes: Android's process lifecycle, Apple's scene phase, the page's visibility. Out of
 * front until the platform first says otherwise, so a process started in the background never opens the socket.
 */
class AppForeground {
    private val isForeground = MutableStateFlow(false)

    fun observe(): StateFlow<Boolean> = isForeground.asStateFlow()

    fun update(isForeground: Boolean) {
        this.isForeground.value = isForeground
    }
}
