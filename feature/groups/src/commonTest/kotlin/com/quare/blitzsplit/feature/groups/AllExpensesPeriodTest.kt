package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.model.ExpensePeriod
import com.quare.blitzsplit.feature.groups.presentation.mapper.periodSpanOrNull
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilters
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the Period filter of "All expenses": the days each period holds, counted in the device's time zone from today.
class AllExpensesPeriodTest {
    @Test
    fun `GIVEN today in mid January WHEN picking last month THEN holds December of the year before whole`() {
        // Given
        val filters = AllExpensesFilters.none.copy(period = ExpensePeriod.LAST_MONTH)
        val today = inBrasilia("2027-01-15T12:00")

        // When
        val span = assertNotNull(filters.periodSpanOrNull(today, brasilia))

        // Then
        assertEquals(
            listOf(false, true, true, false),
            listOf(
                "2026-11-30T23:59",
                "2026-12-01T00:00",
                "2026-12-31T23:59",
                "2027-01-01T00:00",
            ).map { span.holds(inBrasilia(it)) },
        )
    }

    @Test
    fun `GIVEN today in March of a leap year WHEN picking last month THEN holds the 29th of February`() {
        // Given
        val filters = AllExpensesFilters.none.copy(period = ExpensePeriod.LAST_MONTH)
        val today = inBrasilia("2028-03-10T12:00")

        // When
        val span = assertNotNull(filters.periodSpanOrNull(today, brasilia))

        // Then
        assertTrue(span.holds(inBrasilia("2028-02-29T23:30")))
        assertFalse(span.holds(inBrasilia("2028-03-01T00:10")))
    }

    @Test
    fun `GIVEN today the first of the month WHEN picking this month THEN holds today only and not the day before`() {
        // Given
        val filters = AllExpensesFilters.none.copy(period = ExpensePeriod.THIS_MONTH)
        val today = inBrasilia("2026-11-01T08:00")

        // When
        val span = assertNotNull(filters.periodSpanOrNull(today, brasilia))

        // Then
        assertEquals(
            listOf(false, true, true, false),
            listOf(
                "2026-10-31T23:30",
                "2026-11-01T00:00",
                "2026-11-01T23:59",
                "2026-11-02T00:00",
            ).map { span.holds(inBrasilia(it)) },
        )
    }

    @Test
    fun `GIVEN today New Year WHEN picking this year THEN holds today and not New Year Eve`() {
        // Given
        val filters = AllExpensesFilters.none.copy(period = ExpensePeriod.THIS_YEAR)
        val today = inBrasilia("2027-01-01T10:00")

        // When
        val span = assertNotNull(filters.periodSpanOrNull(today, brasilia))

        // Then
        assertTrue(span.holds(inBrasilia("2027-01-01T00:05")))
        assertFalse(span.holds(inBrasilia("2026-12-31T23:55")))
    }

    @Test
    fun `GIVEN the whole history WHEN reading its days THEN there is no span`() {
        // Given
        val filters = AllExpensesFilters.none

        // When
        val span = filters.periodSpanOrNull(inBrasilia("2026-10-31T12:00"), brasilia)

        // Then
        assertNull(span)
    }

    @Test
    fun `GIVEN a custom period without its days WHEN reading its days THEN there is no span`() {
        // Given
        val filters = AllExpensesFilters.none.copy(
            period = ExpensePeriod.CUSTOM,
            customStartMillis = 1L,
        )

        // When
        val span = filters.periodSpanOrNull(inBrasilia("2026-10-31T12:00"), brasilia)

        // Then
        assertNull(span)
    }

    @Test
    fun `GIVEN only a period picked WHEN checking the filters THEN they filter and read every bill`() {
        // Given
        val filters = AllExpensesFilters.none.copy(period = ExpensePeriod.THIS_YEAR)

        // When
        val readsAWindow = filters.readsAWindow

        // Then
        assertFalse(readsAWindow)
        assertTrue(filters.isFiltering)
    }

    @Test
    fun `GIVEN a group and a period picked WHEN checking the filters THEN they read every bill`() {
        // Given
        val filters = AllExpensesFilters.none.copy(
            groupIds = setOf("trio"),
            period = ExpensePeriod.LAST_MONTH,
        )

        // When
        val readsAWindow = filters.readsAWindow

        // Then
        assertFalse(readsAWindow)
    }

    @Test
    fun `GIVEN all time with custom days left WHEN checking the filters THEN they do not filter`() {
        // Given
        val filters = AllExpensesFilters.none.copy(
            customStartMillis = 1L,
            customEndMillis = 2L,
        )

        // When
        val isFiltering = filters.isFiltering

        // Then
        assertFalse(isFiltering)
        assertTrue(filters.readsAWindow)
    }

    // Why: UTC-3 with no summer time, so a moment late in the evening there falls on the next day in UTC.
    private val brasilia = UtcOffset(hours = -3).asTimeZone()

    private fun inBrasilia(dateTime: String): Long = LocalDateTime
        .parse(dateTime)
        .toInstant(brasilia)
        .toEpochMilliseconds()
}
