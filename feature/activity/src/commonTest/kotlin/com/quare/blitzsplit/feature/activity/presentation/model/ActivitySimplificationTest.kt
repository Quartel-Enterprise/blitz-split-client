package com.quare.blitzsplit.feature.activity.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActivitySimplificationTest {
    @Test
    fun `GIVEN changes where two people start owing WHEN reading the new pair THEN is the first of them`() {
        // Given
        val simplification = simplification(
            change("Ana", 5_000L, 1_000L),
            change("Diego", 0L, -3_000L),
            change("Bruno", 0L, 2_000L),
        )

        // When
        val newPair = simplification.newPair

        // Then
        assertEquals(change("Diego", 0L, -3_000L), newPair)
    }

    @Test
    fun `GIVEN changes where two balances go to zero WHEN reading the gone pair THEN is the first of them`() {
        // Given
        val simplification = simplification(
            change("Ana", 5_000L, 1_000L),
            change("Bruno", 3_000L, 0L),
            change("Diego", -2_000L, 0L),
        )

        // When
        val gonePair = simplification.gonePair

        // Then
        assertEquals(change("Bruno", 3_000L, 0L), gonePair)
    }

    @Test
    fun `GIVEN only balances that change amount or side WHEN reading the pairs THEN there is no new nor gone pair`() {
        // Given
        val simplification = simplification(
            change("Ana", 5_000L, 1_000L),
            change("Bruno", 3_000L, -2_000L),
        )

        // When
        val pairs = listOf(simplification.newPair, simplification.gonePair)

        // Then
        assertEquals(listOf(null, null), pairs)
    }

    @Test
    fun `GIVEN nothing changed for you WHEN reading the new pair THEN there is none`() {
        // Given
        val simplification = simplification()

        // When
        val newPair = simplification.newPair

        // Then
        assertNull(newPair)
    }

    @Test
    fun `GIVEN nothing changed for you WHEN reading the gone pair THEN there is none`() {
        // Given
        val simplification = simplification()

        // When
        val gonePair = simplification.gonePair

        // Then
        assertNull(gonePair)
    }

    private fun simplification(vararg changes: ActivityDebtChange): ActivitySimplification = ActivitySimplification(
        paymentsBefore = 7,
        paymentsAfter = 5,
        myChanges = changes.toList(),
    )

    private fun change(
        name: String,
        beforeMinor: Long,
        afterMinor: Long,
    ): ActivityDebtChange = ActivityDebtChange(
        name = name,
        beforeMinor = beforeMinor,
        afterMinor = afterMinor,
        currency = null,
    )
}
