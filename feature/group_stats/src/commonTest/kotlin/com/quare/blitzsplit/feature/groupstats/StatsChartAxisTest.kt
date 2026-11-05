package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.StatsChartAxis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsChartAxisTest {
    @Test
    fun `GIVEN the most expensive day at R 1458 WHEN creating the lines THEN they climb by R 500 to R 1500`() {
        // Given
        val bars = bars(145_800, 30_000, 0)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 50_000L, 100_000L, 150_000L), ticks)
    }

    @Test
    fun `GIVEN the most expensive day exactly on a step WHEN creating the lines THEN that step is the top`() {
        // Given
        val bars = bars(100_000, 20_000)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 50_000L, 100_000L), ticks)
    }

    @Test
    fun `GIVEN one minor unit spent WHEN creating the lines THEN only 0 and 1`() {
        // Given
        val bars = bars(1, 0)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 1L), ticks)
    }

    @Test
    fun `GIVEN the most expensive day at R 50 WHEN creating the lines THEN they climb by R 20`() {
        // Given
        val bars = bars(5_000)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 2_000L, 4_000L, 6_000L), ticks)
    }

    @Test
    fun `GIVEN the most expensive day at R 70 WHEN creating the lines THEN they climb by R 25`() {
        // Given
        val bars = bars(7_000, 1_000)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 2_500L, 5_000L, 7_500L), ticks)
    }

    @Test
    fun `GIVEN seven minor units WHEN creating the lines THEN they climb by 5 never by half a minor unit`() {
        // Given
        val bars = bars(7)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 5L, 10L), ticks)
    }

    @Test
    fun `GIVEN a third of the most expensive day on a power of ten WHEN creating the lines THEN it is the step`() {
        // Given
        val bars = bars(30)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 10L, 20L, 30L), ticks)
    }

    @Test
    fun `GIVEN the most expensive day in the middle WHEN creating the lines THEN they reach it not the last day`() {
        // Given
        val bars = bars(1_000, 7_000, 2_000)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(7_500L, ticks.last())
    }

    @Test
    fun `GIVEN a day of millions WHEN creating the lines THEN they climb by R 500 thousand`() {
        // Given
        val bars = bars(123_456_789, 1_000_000)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, 50_000_000L, 100_000_000L, 150_000_000L), ticks)
    }

    @Test
    fun `GIVEN no bars WHEN creating the lines THEN there are none`() {
        // Given
        val bars = emptyList<StatsBar>()

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertTrue(ticks.isEmpty())
    }

    @Test
    fun `GIVEN only empty days WHEN creating the lines THEN there are none`() {
        // Given
        val bars = bars(0, 0, 0)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertTrue(ticks.isEmpty())
    }

    @Test
    fun `GIVEN any largest day WHEN creating the lines THEN at most three equal steps up to the first above it`() {
        // Given
        val largests = (1L..20_000L) + listOf(99_999L, 145_800L, 1_000_001L, 987_654_321L)

        // When
        val ticksByLargest = largests.associateWith { StatsChartAxis.createTicks(bars(it)) }

        // Then
        ticksByLargest.forEach { (largest, ticks) ->
            val step = ticks[1]
            assertEquals(0L, ticks.first(), "largest $largest")
            assertTrue(ticks.size in 2..4, "largest $largest: $ticks")
            assertEquals(ticks.indices.map { it * step }, ticks, "largest $largest")
            assertTrue(ticks.last() >= largest, "largest $largest: $ticks")
            assertTrue(ticks[ticks.size - 2] < largest, "largest $largest: $ticks")
        }
    }

    @Test
    fun `GIVEN any largest day WHEN creating the lines THEN steps by the smallest round amount of a third or more`() {
        // Given
        val largests = (1L..20_000L) + listOf(99_999L, 145_800L, 1_000_001L, 987_654_321L)

        // When
        val stepsByLargest = largests.associateWith { StatsChartAxis.createTicks(bars(it))[1] }

        // Then
        stepsByLargest.forEach { (largest, step) ->
            val third = (largest + 2) / 3
            val expected = roundSteps().first { it >= third }
            assertEquals(expected, step, "largest $largest")
        }
    }

    @Test
    fun `GIVEN bills on the first and last of five days WHEN averaging THEN the empty days count too`() {
        // Given
        val bars = bars(30_000, 0, 0, 0, 60_000)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertEquals(18_000L, average)
    }

    @Test
    fun `GIVEN a mean of half a minor unit WHEN averaging THEN rounds up`() {
        // Given
        val bars = bars(1, 2)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertEquals(2L, average)
    }

    @Test
    fun `GIVEN a mean under half a minor unit WHEN averaging THEN rounds down`() {
        // Given
        val bars = bars(5, 0, 0, 0)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertEquals(1L, average)
    }

    @Test
    fun `GIVEN a mean under half of one minor unit WHEN averaging THEN there is no line`() {
        // Given
        val bars = bars(1, 0, 0)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertNull(average)
    }

    @Test
    fun `GIVEN days of millions WHEN averaging THEN rounds without losing a unit`() {
        // Given
        val bars = bars(999_999_999, 0)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertEquals(500_000_000L, average)
    }

    @Test
    fun `GIVEN a single day WHEN averaging THEN there is no average`() {
        // Given
        val bars = bars(60_000)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertNull(average)
    }

    @Test
    fun `GIVEN no bars WHEN averaging THEN there is no average`() {
        // Given
        val bars = emptyList<StatsBar>()

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertNull(average)
    }

    @Test
    fun `GIVEN only empty days WHEN averaging THEN there is no average`() {
        // Given
        val bars = bars(0, 0, 0)

        // When
        val average = StatsChartAxis.createAverageOrNull(bars)

        // Then
        assertNull(average)
    }

    // Why: 1, 2, 2.5 and 5 of each power of ten, 2.5 only once it is a whole number of minor units, smallest first.
    private fun roundSteps(): List<Long> = generateSequence(1L) { it * 10 }
        .take(TEN_POWERS)
        .flatMap { power ->
            listOfNotNull(power, 2 * power, (5 * power / 2).takeIf { power >= 10 }, 5 * power)
        }.toList()

    private fun bars(vararg amounts: Long): List<StatsBar> = amounts.mapIndexed { index, amount ->
        StatsBar(
            startMillis = index * DAY_MILLIS,
            endMillis = index * DAY_MILLIS,
            amountMinor = amount,
            expenseCount = if (amount > 0) 1 else 0,
            isToday = false,
            isPeak = false,
            showsLabel = true,
            showsDot = true,
        )
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000L
        const val TEN_POWERS = 12
    }

    @Test
    fun `GIVEN an amount near the largest Long WHEN creating the lines THEN one top line stands for the scale`() {
        // Given
        val bars = bars(Long.MAX_VALUE / 2)

        // When
        val ticks = StatsChartAxis.createTicks(bars)

        // Then
        assertEquals(listOf(0L, Long.MAX_VALUE / 2), ticks)
    }
}
