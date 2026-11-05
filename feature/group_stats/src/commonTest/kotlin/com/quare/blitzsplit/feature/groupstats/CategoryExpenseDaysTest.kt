package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.CategoryExpenseDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryExpenseDaysTest {
    // Why: Thursday, 29 October 2026.
    private val today = LocalDate(2026, 10, 29)

    @Test
    fun `GIVEN last month from its first to its last day WHEN writing the period THEN it is a month`() {
        // Given
        val span = DateSpan(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.MONTH, style)
    }

    @Test
    fun `GIVEN this month from its first day to today WHEN writing the period THEN it is a month`() {
        // Given
        val span = DateSpan(LocalDate(2026, 10, 1), today)

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.MONTH, style)
    }

    @Test
    fun `GIVEN February of a common year WHEN writing the period THEN it is a month`() {
        // Given
        val span = DateSpan(LocalDate(2026, 2, 1), LocalDate(2026, 2, 28))

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.MONTH, style)
    }

    @Test
    fun `GIVEN the first ten days of this month before today WHEN writing the period THEN it is days`() {
        // Given
        val span = DateSpan(LocalDate(2026, 10, 1), LocalDate(2026, 10, 10))

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.DAYS, style)
    }

    @Test
    fun `GIVEN days from the middle of the month to its end WHEN writing the period THEN it is days`() {
        // Given
        val span = DateSpan(LocalDate(2026, 9, 5), LocalDate(2026, 9, 30))

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.DAYS, style)
    }

    @Test
    fun `GIVEN the first of a month to today in the next month WHEN writing the period THEN it is days`() {
        // Given
        val span = DateSpan(LocalDate(2026, 9, 1), today)

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.DAYS, style)
    }

    @Test
    fun `GIVEN exactly three months WHEN writing the period THEN it is still days`() {
        // Given
        val span = DateSpan(LocalDate(2026, 7, 29), today)

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.DAYS, style)
    }

    @Test
    fun `GIVEN three months and a day WHEN writing the period THEN it is months`() {
        // Given
        val span = DateSpan(LocalDate(2026, 7, 28), today)

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.MONTHS, style)
    }

    @Test
    fun `GIVEN two years WHEN writing the period THEN it is months`() {
        // Given
        val span = DateSpan(LocalDate(2024, 11, 1), today)

        // When
        val style = CategoryExpenseDays.rangeStyleOf(span, today)

        // Then
        assertEquals(StatsRangeStyle.MONTHS, style)
    }
}
