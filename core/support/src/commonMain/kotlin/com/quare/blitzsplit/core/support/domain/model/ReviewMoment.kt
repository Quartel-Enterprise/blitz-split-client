package com.quare.blitzsplit.core.support.domain.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: something that just went well, after which the app may ask for a review in the store.
sealed interface ReviewMoment {
    // Why: what in_app_review_requested says led to it.
    val trigger: String

    // Why: a new bill saved on this device; an edit is none. The third one on is a moment to ask at.
    data object BillCreated : ReviewMoment {
        override val trigger: String = "bill_saved"
    }

    // Why: a payment recorded in these groups, which is a moment to ask at only when it leaves one with nobody owing.
    data class PaymentRecorded(
        val groupIds: List<String>,
    ) : ReviewMoment {
        override val trigger: String = "group_settled"
    }

    companion object {
        // Why: no UiEvent stands for it; RequestReviewIfNeededUseCase sends it by hand once the store is asked.
        val analytics: EventAnalytics = EventAnalytics.Manual("in_app_review_requested")
    }
}
