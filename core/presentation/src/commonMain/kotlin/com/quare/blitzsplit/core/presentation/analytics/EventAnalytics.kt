package com.quare.blitzsplit.core.presentation.analytics

/*
 * Why: what a user action sends to Analytics (docs/analytics/README.md), declared on its UiEvent so the compiler asks
 * for the decision on every new one. Track when it is a user action, NotTracked only when it isn't one at all.
 */
sealed interface EventAnalytics {
    sealed interface Track : EventAnalytics

    /*
     * Why: the event and its params are known from the UiEvent itself, so TrackedViewModel sends it before handling the
     * event; the ViewModel never calls trackEvent for it.
     */
    data class Automatic(
        val name: String,
        val params: Map<String, Any>,
    ) : Track {
        constructor(name: String) : this(name, emptyMap())
    }

    /*
     * Why: the event needs what only handling it tells (the result of a save, the UiState), so the ViewModel calls
     * trackEvent itself; names lists every event that branch may send, which the catalog test finds wired.
     */
    data class Manual(
        val names: Set<String>,
    ) : Track {
        constructor(name: String) : this(setOf(name))
    }

    // Why: not a user action at all: a scroll, an animation ending, a frame measured, a value the UI hands back.
    data object NotTracked : EventAnalytics
}
