package com.quare.blitzsplit.shared.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/*
 * Why: a link the system opens the app with (an invite) may come before the app's first screen is in place, or
 * before anything observes it, so it waits here until the app takes it.
 */
class LinkInbox {
    private val pending = MutableStateFlow<String?>(null)

    fun receive(url: String) {
        pending.value = url
    }

    fun observe(): Flow<String> = pending.filterNotNull()

    fun take(url: String) {
        pending.compareAndSet(url, null)
    }
}
