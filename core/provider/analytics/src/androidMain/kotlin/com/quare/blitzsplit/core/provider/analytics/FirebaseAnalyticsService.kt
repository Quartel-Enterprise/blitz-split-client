package com.quare.blitzsplit.core.provider.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

// Why: Android's Firebase Analytics, which the app attaches once Firebase starts (BlitzSplitApplication).
class FirebaseAnalyticsService(
    context: Context,
) : AnalyticsService {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        analytics.logEvent(name, params.toBundle())
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().also { bundle ->
        forEach { (key, value) ->
            when (value) {
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
    }
}
