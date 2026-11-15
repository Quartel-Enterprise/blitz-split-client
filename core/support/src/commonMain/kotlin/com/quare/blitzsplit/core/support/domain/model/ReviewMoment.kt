package com.quare.blitzsplit.core.support.domain.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: something that just went well, after which the app may ask for a review in the store.
sealed interface ReviewMoment {
    // Why: the event sent once the store is asked, which says what led to it.
    val analytics: EventAnalytics.Automatic

    // Why: a new bill saved on this device; an edit is none. The third one on is a moment to ask at.
    data object BillCreated : ReviewMoment {
        override val analytics: EventAnalytics.Automatic =
            EventAnalytics.Automatic("in_app_review_requested", mapOf("trigger" to "bill_saved"))
    }

    // Why: a payment recorded in these groups, which is a moment to ask at only when it leaves one with nobody owing.
    data class PaymentRecorded(
        val groupIds: List<String>,
    ) : ReviewMoment {
        override val analytics: EventAnalytics.Automatic =
            EventAnalytics.Automatic("in_app_review_requested", mapOf("trigger" to "group_settled"))
    }
}
