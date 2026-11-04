package com.quare.blitzsplit.core.provider.analytics

/*
 * Why: GA4 takes strings and numbers only, so a Boolean goes as "true" or "false", a whole number as a Long, any
 * other as a Double and an enum as its name in lowercase, the same on every platform. Any other value is left out
 * rather than sent as its text, which for a domain object would carry names, amounts and ids.
 */
internal class TrackEventUseCase(
    private val hub: AnalyticsHub,
) : TrackEvent {
    override fun invoke(
        name: String,
        params: Map<String, Any>,
    ) {
        val values = params.mapNotNull { (key, value) -> value.toAnalyticsValue()?.let { key to it } }.toMap()
        hub.logEvent(AnalyticsEvent(name, values))
    }

    private fun Any.toAnalyticsValue(): Any? = when (this) {
        is String, is Long -> this
        is Boolean -> toString()
        is Number -> toWholeOrDouble()
        is Enum<*> -> name.lowercase()
        else -> null
    }

    // Why: by the value, not the type, since Kotlin/JS can't tell an Int from a Double.
    private fun Number.toWholeOrDouble(): Any {
        val double = toDouble()
        val whole = toLong()
        return if (whole.toDouble() == double) whole else double
    }
}
