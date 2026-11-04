package com.quare.blitzsplit.core.provider.analytics

/*
 * Why: how the shared code tracks a user action (docs/analytics/README.md). Never a name, an amount nor an id in
 * params: the event goes to Google.
 */
fun interface TrackEvent {
    operator fun invoke(
        name: String,
        params: Map<String, Any>,
    )
}
