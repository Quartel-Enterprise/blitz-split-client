package com.quare.blitzsplit.core.provider.analytics

/*
 * Why: GA4 takes strings and numbers only, so a Boolean goes as "true" or "false", an Int as a Long, a Float as a
 * Double and anything else as its text, the same on every platform.
 */
internal class TrackEventUseCase(
    private val hub: AnalyticsHub,
) : TrackEvent {
    override fun invoke(
        name: String,
        params: Map<String, Any>,
    ) {
        hub.logEvent(name, params.mapValues { (_, value) -> value.toAnalyticsValue() })
    }

    private fun Any.toAnalyticsValue(): Any = when (this) {
        is String, is Long, is Double -> this
        is Int -> toLong()
        is Short -> toLong()
        is Byte -> toLong()
        is Float -> toDouble()
        else -> toString()
    }
}
