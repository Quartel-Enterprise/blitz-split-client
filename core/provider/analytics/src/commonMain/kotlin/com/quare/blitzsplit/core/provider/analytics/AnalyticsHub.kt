package com.quare.blitzsplit.core.provider.analytics

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/*
 * Why: the shared code tracks from startup, while each app attaches its platform's service a moment later (the web
 * only once the page drew and the browser said it supports it), and only in a release build with the Firebase config.
 * Until then the first events wait, so the opening screen counts on every platform; a build that never attaches keeps
 * at most a handful, and the jvm tests none that matter.
 */
@OptIn(ExperimentalAtomicApi::class)
class AnalyticsHub {
    @Volatile
    private var service: AnalyticsService? = null
    private val pending = AtomicReference<List<AnalyticsEvent>>(emptyList())

    fun attach(service: AnalyticsService) {
        this.service = service
        sendPending(service)
    }

    internal fun logEvent(event: AnalyticsEvent) {
        val attached = service
        if (attached != null) {
            // Why: the queue attach is still sending goes first, so a newer event never overtakes the opening screen.
            sendPending(attached)
            attached.send(event)
            return
        }
        enqueue(event)
        // Why: a service attached while the event was being queued has already sent the queue it found.
        service?.let(::sendPending)
    }

    private fun enqueue(event: AnalyticsEvent) {
        while (true) {
            val events = pending.load()
            if (events.size >= MAX_PENDING || pending.compareAndSet(events, events + event)) return
        }
    }

    private fun sendPending(service: AnalyticsService) {
        pending.exchange(emptyList()).forEach { event -> service.send(event) }
    }

    // Why: Analytics only counts what people do: an SDK that throws loses that event, never the tap nor the screen.
    private fun AnalyticsService.send(event: AnalyticsEvent) {
        runCatching { logEvent(event.name, event.params) }
    }

    private companion object {
        const val MAX_PENDING = 50
    }
}
