package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SpentDayTest {
    // Why: São Paulo, with no daylight saving since 2019, as a fixed offset: the JS target has no time-zone database.
    private val saoPaulo = UtcOffset(hours = -3).asTimeZone()

    // Why: 22:00 of the 29th in São Paulo, while UTC is already on the 30th.
    private val lateEveningInSaoPaulo = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()

    @Test
    fun `GIVEN a late evening bill in Sao Paulo WHEN reading its day there THEN it is the local day`() {
        // Given
        val epochMillis = lateEveningInSaoPaulo

        // When
        val date = SpentDay.dateOf(epochMillis, saoPaulo)

        // Then
        assertEquals(LocalDate(2026, 10, 29), date)
    }

    @Test
    fun `GIVEN the same instant WHEN reading its day in UTC THEN it is the next day`() {
        // Given
        val epochMillis = lateEveningInSaoPaulo

        // When
        val date = SpentDay.dateOf(epochMillis, TimeZone.UTC)

        // Then
        assertEquals(LocalDate(2026, 10, 30), date)
    }

    @Test
    fun `GIVEN a day in Sao Paulo WHEN reading its start THEN it is local midnight`() {
        // Given
        val date = LocalDate(2026, 10, 29)

        // When
        val start = SpentDay.startOf(date, saoPaulo)

        // Then
        assertEquals(Instant.parse("2026-10-29T03:00:00Z").toEpochMilliseconds(), start)
    }

    @Test
    fun `GIVEN the start of a day WHEN reading its day back THEN it is the same day`() {
        // Given
        val start = SpentDay.startOf(LocalDate(2026, 3, 1), saoPaulo)

        // When
        val date = SpentDay.dateOf(start, saoPaulo)

        // Then
        assertEquals(LocalDate(2026, 3, 1), date)
    }

    @Test
    fun `GIVEN the local day while UTC is on the next WHEN naming it THEN it is today`() {
        // Given
        val date = LocalDate(2026, 10, 29)

        // When
        val relativeDay = SpentDay.relativeDayOf(
            date,
            nowMillis = lateEveningInSaoPaulo,
            timeZone = saoPaulo,
        )

        // Then
        assertEquals(RelativeDay.TODAY, relativeDay)
    }

    @Test
    fun `GIVEN the day before the local day WHEN naming it THEN it is yesterday`() {
        // Given
        val date = LocalDate(2026, 10, 28)

        // When
        val relativeDay = SpentDay.relativeDayOf(
            date,
            nowMillis = lateEveningInSaoPaulo,
            timeZone = saoPaulo,
        )

        // Then
        assertEquals(RelativeDay.YESTERDAY, relativeDay)
    }

    @Test
    fun `GIVEN the UTC day that is still tomorrow in Sao Paulo WHEN naming it THEN it is another day`() {
        // Given
        val date = LocalDate(2026, 10, 30)

        // When
        val relativeDay = SpentDay.relativeDayOf(
            date,
            nowMillis = lateEveningInSaoPaulo,
            timeZone = saoPaulo,
        )

        // Then
        assertEquals(RelativeDay.OTHER, relativeDay)
    }

    @Test
    fun `GIVEN two days before WHEN naming the day THEN it is another day`() {
        // Given
        val date = LocalDate(2026, 10, 27)

        // When
        val relativeDay = SpentDay.relativeDayOf(
            date,
            nowMillis = lateEveningInSaoPaulo,
            timeZone = saoPaulo,
        )

        // Then
        assertEquals(RelativeDay.OTHER, relativeDay)
    }

    @Test
    fun `GIVEN new year day WHEN naming the last day of the year before THEN it is yesterday`() {
        // Given
        val date = LocalDate(2026, 12, 31)
        val newYearNoon = Instant.parse("2027-01-01T12:00:00Z").toEpochMilliseconds()

        // When
        val relativeDay = SpentDay.relativeDayOf(
            date,
            nowMillis = newYearNoon,
            timeZone = TimeZone.UTC,
        )

        // Then
        assertEquals(RelativeDay.YESTERDAY, relativeDay)
    }
}
