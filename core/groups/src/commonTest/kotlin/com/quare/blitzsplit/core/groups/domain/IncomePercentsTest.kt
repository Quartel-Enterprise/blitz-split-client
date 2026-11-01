package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.IncomePercents
import kotlin.test.Test
import kotlin.test.assertEquals

class IncomePercentsTest {
    @Test
    fun `GIVEN 9000 and 3000 WHEN working out the percentages THEN 75 and 25`() {
        // Given
        val incomes = listOf("pierre" to 900_000L, "ana" to 300_000L)

        // When
        val percents = IncomePercents.of(incomes)

        // Then
        assertEquals(mapOf("pierre" to 75L, "ana" to 25L), percents)
    }

    @Test
    fun `GIVEN three equal incomes WHEN working out the percentages THEN the point left goes to the first`() {
        // Given
        val incomes = listOf("a" to 100L, "b" to 100L, "c" to 100L)

        // When
        val percents = IncomePercents.of(incomes)

        // Then
        assertEquals(listOf(34L, 33L, 33L), percents.values.toList())
    }

    @Test
    fun `GIVEN uneven incomes WHEN working out the percentages THEN the largest remainders round up`() {
        // Given
        val incomes = listOf("a" to 1L, "b" to 2L, "c" to 4L)

        // When
        val percents = IncomePercents.of(incomes)

        // Then
        assertEquals(mapOf("a" to 14L, "b" to 29L, "c" to 57L), percents)
    }

    @Test
    fun `GIVEN nobody with an income WHEN working out the percentages THEN everyone is at 0`() {
        // Given
        val incomes = listOf("a" to 0L, "b" to 0L)

        // When
        val percents = IncomePercents.of(incomes)

        // Then
        assertEquals(mapOf("a" to 0L, "b" to 0L), percents)
    }
}
