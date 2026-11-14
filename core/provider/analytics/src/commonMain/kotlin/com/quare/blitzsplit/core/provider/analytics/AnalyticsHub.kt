package com.quare.blitzsplit.core.provider.analytics

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/*
 * Why: the shared code tracks from startup, while each app attaches its platform's service a moment later (the web
 * only once the page drew and the browser said it supports it), and only in a release build with the Firebase config.
 * The device's "Share usage data" is read from Room a moment later too. Until both are there the first events wait,
 * so the opening screen counts on every platform and nothing leaves a device that turned sharing off; a build that
 * never attaches keeps at most a handful, and the jvm tests none that matter.
 */
@OptIn(ExperimentalAtomicApi::class)
class AnalyticsHub {
    @Volatile
    private var service: AnalyticsService? = null

    // Why: null until the device's choice is read, when events wait instead of going out or being dropped.
    @Volatile
    private var isCollectionEnabled: Boolean? = null

    @Volatile
    private var userId: String? = null
    private val pending = AtomicReference<List<AnalyticsEvent>>(emptyList())

    // Why: the SDK gets the id and the choice before any thread can send through it, so no event beats them.
    fun attach(service: AnalyticsService) {
        val configuredId = userId
        val configuredChoice = isCollectionEnabled
        service.safely { setUserId(configuredId) }
        configuredChoice?.let { isEnabled -> service.safely { setCollectionEnabled(isEnabled) } }
        this.service = service
        // Why: an id or a choice that changed meanwhile found no service yet, so it is handed over now.
        val latestId = userId
        if (latestId != configuredId) service.safely { setUserId(latestId) }
        val latestChoice = isCollectionEnabled
        if (latestChoice != null &&
            latestChoice != configuredChoice
        ) {
            service.safely { setCollectionEnabled(latestChoice) }
        }
        flush()
    }

    fun setCollectionEnabled(isEnabled: Boolean) {
        isCollectionEnabled = isEnabled
        service?.safely { setCollectionEnabled(isEnabled) }
        if (isEnabled) flush() else pending.store(emptyList())
    }

    fun setUserId(userId: String?) {
        this.userId = userId
        service?.safely { setUserId(userId) }
    }

    internal fun logEvent(event: AnalyticsEvent) {
        when (isCollectionEnabled) {
            false -> return

            true -> service?.let { attached ->
                // Why: the queue attach is still sending goes first, so a newer event never overtakes the opening screen.
                sendPending(attached)
                attached.send(event)
                return
            }

            null -> Unit
        }
        enqueue(event)
        // Why: a service attached, or the choice read, while the event was queued already sent the queue it found.
        flush()
    }

    private fun flush() {
        val attached = service ?: return
        when (isCollectionEnabled) {
            true -> sendPending(attached)
            false -> pending.store(emptyList())
            null -> Unit
        }
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
        safely { logEvent(event.name, event.params) }
    }

    private inline fun AnalyticsService.safely(block: AnalyticsService.() -> Unit) {
        runCatching { block() }
    }

    private companion object {
        const val MAX_PENDING = 50
    }
}
