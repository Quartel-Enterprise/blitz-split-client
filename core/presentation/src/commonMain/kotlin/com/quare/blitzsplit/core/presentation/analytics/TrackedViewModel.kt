package com.quare.blitzsplit.core.presentation.analytics

import androidx.lifecycle.ViewModel
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent

/*
 * Why: the ViewModel of a screen whose UiEvent is a TrackedUiEvent. Every UI calls onEvent, which sends an Automatic
 * event before handleEvent runs, so the three UIs send the same events and no ViewModel repeats the call.
 */
abstract class TrackedViewModel<E : TrackedUiEvent>(
    protected val trackEvent: TrackEvent,
) : ViewModel() {
    fun onEvent(event: E) {
        if (!isAccepted(event)) return
        val analytics = event.analytics
        if (analytics is EventAnalytics.Automatic) trackEvent(analytics.name, analytics.params)
        handleEvent(event)
    }

    protected abstract fun handleEvent(event: E)

    /*
     * Why: a screen that refuses whole kinds of events for a while (Rachar conta while it reads a receipt) says so,
     * and those events are neither tracked nor handled, so no tap the screen didn't take ever counts.
     */
    protected open fun isAccepted(event: E): Boolean = true
}
