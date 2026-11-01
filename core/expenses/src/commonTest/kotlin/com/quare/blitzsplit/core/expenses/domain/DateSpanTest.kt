package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.model.StatsGrain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateSpanTest {
    @Test
    fun `GIVEN three months to the day WHEN checking Dia THEN fits`() {
        // Given
        val span = DateSpan(LocalDate(2026, 7, 29), LocalDate(2026, 10, 29))

        // When
        val fits = span.canSplitBy(StatsGrain.DAY)

        // Then
        assertTrue(fits)
    }

    @Test
    fun `GIVEN three months and a day WHEN checking Dia THEN does not fit`() {
        // Given
        val span = DateSpan(LocalDate(2026, 7, 28), LocalDate(2026, 10, 29))

        // When
        val fits = span.canSplitBy(StatsGrain.DAY)

        // Then
        assertFalse(fits)
    }

    @Test
    fun `GIVEN a Saturday to the next Thursday WHEN checking Semana THEN fits two weeks`() {
        // Given
        val span = DateSpan(LocalDate(2026, 10, 24), LocalDate(2026, 10, 29))

        // When
        val fits = span.canSplitBy(StatsGrain.WEEK)

        // Then
        assertTrue(fits)
    }

    @Test
    fun `GIVEN Monday to Sunday WHEN checking Semana THEN is a single week`() {
        // Given
        val span = DateSpan(LocalDate(2026, 10, 26), LocalDate(2026, 11, 1))

        // When
        val fits = span.canSplitBy(StatsGrain.WEEK)

        // Then
        assertFalse(fits)
    }

    @Test
    fun `GIVEN days of two months in one week WHEN checking Mes and Ano THEN only Mes fits`() {
        // Given
        val span = DateSpan(LocalDate(2026, 10, 31), LocalDate(2026, 11, 1))

        // When
        val fits = listOf(span.canSplitBy(StatsGrain.MONTH), span.canSplitBy(StatsGrain.YEAR))

        // Then
        assertEquals(listOf(true, false), fits)
    }

    @Test
    fun `GIVEN December to January WHEN checking Ano THEN fits two years`() {
        // Given
        val span = DateSpan(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1))

        // When
        val fits = span.canSplitBy(StatsGrain.YEAR)

        // Then
        assertTrue(fits)
    }

    @Test
    fun `GIVEN a trip WHEN splitting by week THEN each week runs Monday to Sunday whole`() {
        // Given
        val span = DateSpan(LocalDate(2026, 10, 24), LocalDate(2026, 10, 29))

        // When
        val weeks = span.splitBy(StatsGrain.WEEK)

        // Then
        assertEquals(
            listOf(
                DateSpan(LocalDate(2026, 10, 19), LocalDate(2026, 10, 25)),
                DateSpan(LocalDate(2026, 10, 26), LocalDate(2026, 11, 1)),
            ),
            weeks,
        )
    }

    @Test
    fun `GIVEN March 2024 to October 2026 WHEN splitting by month and year THEN counts every month and year`() {
        // Given
        val span = DateSpan(LocalDate(2024, 3, 5), LocalDate(2026, 10, 29))

        // When
        val months = span.splitBy(StatsGrain.MONTH)
        val years = span.splitBy(StatsGrain.YEAR)

        // Then
        assertEquals(32, months.size)
        assertEquals(DateSpan(LocalDate(2024, 3, 1), LocalDate(2024, 3, 31)), months.first())
        assertEquals(DateSpan(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)), months.last())
        assertEquals(listOf(2024, 2025, 2026), years.map { it.start.year })
    }

    @Test
    fun `GIVEN three days WHEN splitting by day THEN gives each day`() {
        // Given
        val span = DateSpan(LocalDate(2026, 2, 27), LocalDate(2026, 3, 1))

        // When
        val days = span.splitBy(StatsGrain.DAY)

        // Then
        assertEquals(listOf(27, 28, 1), days.map { it.start.day })
    }
}
