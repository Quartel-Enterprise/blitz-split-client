package com.quare.blitzsplit.core.provider.analytics

/*
 * Why: the shared code tracks from startup, while each app attaches its platform's service only in a build with the
 * Firebase config and never in a debug one: until then, and on the jvm tests, an event goes nowhere.
 */
class AnalyticsHub {
    private var service: AnalyticsService? = null

    fun attach(service: AnalyticsService) {
        this.service = service
    }

    internal fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        service?.logEvent(name, params)
    }
}
