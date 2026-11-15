package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewMomentTest {
    @Test
    fun `GIVEN a bill created WHEN reading its analytics THEN the review was requested by a saved bill`() {
        // Given
        val moment = ReviewMoment.BillCreated

        // When
        val analytics = moment.analytics

        // Then
        assertEquals(EventAnalytics.Automatic("in_app_review_requested", mapOf("trigger" to "bill_saved")), analytics)
    }

    @Test
    fun `GIVEN a payment recorded WHEN reading its analytics THEN the review was requested by a settled group`() {
        // Given
        val moment = ReviewMoment.PaymentRecorded(listOf("group-trip"))

        // When
        val analytics = moment.analytics

        // Then
        assertEquals(
            EventAnalytics.Automatic("in_app_review_requested", mapOf("trigger" to "group_settled")),
            analytics,
        )
    }
}
