@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.AnalyticsService
import org.koin.mp.KoinPlatform

/*
 * Why: Firebase Analytics on the web (webApp/src/analytics), attached only by a production build with its Firebase
 * config. Each event's params cross as a plain object of strings and numbers, since a Kotlin Map or Long doesn't.
 */
fun attachAnalytics(logEvent: (name: String, params: dynamic) -> Unit) {
    KoinPlatform.getKoin().get<AnalyticsHub>().attach(WebAnalyticsService(logEvent))
}

private class WebAnalyticsService(
    private val logEvent: (name: String, params: dynamic) -> Unit,
) : AnalyticsService {
    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        val plain: dynamic = js("({})")
        params.forEach { (key, value) ->
            plain[key] = if (value is Long) value.toDouble() else value
        }
        logEvent(name, plain)
    }
}
