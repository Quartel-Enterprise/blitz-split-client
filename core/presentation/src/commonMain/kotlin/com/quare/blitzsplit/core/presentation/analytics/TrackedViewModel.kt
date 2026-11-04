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
        val analytics = event.analytics
        if (analytics is EventAnalytics.Automatic && isAccepted(event)) trackEvent(analytics.name, analytics.params)
        handleEvent(event)
    }

    protected abstract fun handleEvent(event: E)

    /*
     * Why: a screen that refuses whole kinds of events for a while (Rachar conta while it reads a receipt) says so,
     * so their Automatic events never count a tap the screen didn't take.
     */
    protected open fun isAccepted(event: E): Boolean = true
}
