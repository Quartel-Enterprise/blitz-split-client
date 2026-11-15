package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewMomentTest {
    @Test
    fun `GIVEN a bill created WHEN reading its trigger THEN a saved bill led to the review`() {
        // Given
        val moment = ReviewMoment.BillCreated

        // When
        val trigger = moment.trigger

        // Then
        assertEquals("bill_saved", trigger)
    }

    @Test
    fun `GIVEN a payment recorded WHEN reading its trigger THEN a settled group led to the review`() {
        // Given
        val moment = ReviewMoment.PaymentRecorded(listOf("group-trip"))

        // When
        val trigger = moment.trigger

        // Then
        assertEquals("group_settled", trigger)
    }

    @Test
    fun `GIVEN the review moments WHEN reading their analytics THEN the event is sent by hand`() {
        // Given
        val moments = ReviewMoment

        // When
        val analytics = moments.analytics

        // Then
        assertEquals(EventAnalytics.Manual("in_app_review_requested"), analytics)
    }
}
