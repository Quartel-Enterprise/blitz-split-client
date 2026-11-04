package com.quare.blitzsplit.core.provider.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile

/*
 * Why: the shared code tracks from startup, while each app attaches its platform's service a moment later (the web
 * only once the page drew and the browser said it supports it), and only in a release build with the Firebase config.
 * Until then the first events wait, so the opening screen counts on every platform; a build that never attaches keeps
 * at most a handful, and the jvm tests none that matter.
 */
class AnalyticsHub {
    @Volatile
    private var service: AnalyticsService? = null
    private val pending = MutableStateFlow<List<AnalyticsEvent>>(emptyList())

    fun attach(service: AnalyticsService) {
        this.service = service
        sendPending(service)
    }

    internal fun logEvent(event: AnalyticsEvent) {
        val attached = service
        if (attached != null) {
            attached.logEvent(event.name, event.params)
            return
        }
        pending.update { events -> if (events.size < MAX_PENDING) events + event else events }
        // Why: a service attached while the event was being queued has already sent the queue it found.
        service?.let(::sendPending)
    }

    private fun sendPending(service: AnalyticsService) {
        pending.getAndUpdate { emptyList() }.forEach { event -> service.logEvent(event.name, event.params) }
    }

    private companion object {
        const val MAX_PENDING = 50
    }
}
