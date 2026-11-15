package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.impl.ReviewMoments
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewMomentsTest {
    private val moments = ReviewMoments()
    private val observed = mutableListOf<ReviewMoment>()

    @Test
    fun `GIVEN someone observing WHEN a moment is noted THEN it reaches them`() = runTest {
        // Given
        observeMoments()

        // When
        moments(ReviewMoment.BillCreated)
        runCurrent()

        // Then
        assertEquals(listOf<ReviewMoment>(ReviewMoment.BillCreated), observed)
    }

    @Test
    fun `GIVEN a moment already observed WHEN another is noted THEN both arrive in the order noted`() = runTest {
        // Given
        val payment = ReviewMoment.PaymentRecorded(listOf("group-trip"))
        observeMoments()
        moments(ReviewMoment.BillCreated)
        runCurrent()

        // When
        moments(payment)
        runCurrent()

        // Then
        assertEquals(listOf(ReviewMoment.BillCreated, payment), observed)
    }

    @Test
    fun `GIVEN a moment noted with nobody observing WHEN someone starts observing THEN it is not replayed`() = runTest {
        // Given
        moments(ReviewMoment.BillCreated)

        // When
        observeMoments()

        // Then
        assertEquals(emptyList(), observed)
    }

    @Test
    fun `GIVEN a moment dropped with nobody observing WHEN a later one is noted THEN only the later one arrives`() =
        runTest {
            // Given
            val payment = ReviewMoment.PaymentRecorded(listOf("group-trip"))
            moments(ReviewMoment.BillCreated)
            observeMoments()

            // When
            moments(payment)
            runCurrent()

            // Then
            assertEquals(listOf<ReviewMoment>(payment), observed)
        }

    private fun TestScope.observeMoments() {
        backgroundScope.launch { moments().collect(observed::add) }
        runCurrent()
    }
}
