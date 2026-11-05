package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupStatsUiStateTest {
    @Test
    fun `GIVEN value lines up to R 1500 WHEN reading the chart top THEN it is the last line`() {
        // Given
        val state = state(
            ticks = listOf(0L, 50_000L, 100_000L, 150_000L),
            amounts = listOf(145_800L, 30_000L),
        )

        // When
        val top = state.chartTopMinor

        // Then
        assertEquals(150_000L, top)
    }

    @Test
    fun `GIVEN no value lines WHEN reading the chart top THEN it is the most expensive bar`() {
        // Given
        val state = state(
            ticks = emptyList(),
            amounts = listOf(200L, 700L, 0L),
        )

        // When
        val top = state.chartTopMinor

        // Then
        assertEquals(700L, top)
    }

    @Test
    fun `GIVEN only the zero line WHEN reading the chart top THEN it is the most expensive bar`() {
        // Given
        val state = state(
            ticks = listOf(0L),
            amounts = listOf(500L, 100L),
        )

        // When
        val top = state.chartTopMinor

        // Then
        assertEquals(500L, top)
    }

    @Test
    fun `GIVEN only empty bars WHEN reading the chart top THEN it is one minor unit`() {
        // Given
        val state = state(
            ticks = emptyList(),
            amounts = listOf(0L, 0L),
        )

        // When
        val top = state.chartTopMinor

        // Then
        assertEquals(1L, top)
    }

    @Test
    fun `GIVEN no bars WHEN reading the chart top THEN it is one minor unit`() {
        // Given
        val state = state(
            ticks = emptyList(),
            amounts = emptyList(),
        )

        // When
        val top = state.chartTopMinor

        // Then
        assertEquals(1L, top)
    }

    private fun bar(
        index: Int,
        amountMinor: Long,
    ): StatsBar = StatsBar(
        startMillis = index * DAY_MILLIS,
        endMillis = index * DAY_MILLIS,
        amountMinor = amountMinor,
        expenseCount = if (amountMinor > 0) 1 else 0,
        isToday = false,
        isPeak = false,
        showsLabel = true,
        showsDot = true,
    )

    private fun state(
        ticks: List<Long>,
        amounts: List<Long>,
    ): GroupStatsUiState = GroupStatsUiState(
        groupName = "Viagem Floripa",
        currency = GroupCurrency.BRL,
        isTrip = true,
        period = StatsPeriod.ALL,
        periodStartMillis = 0L,
        periodEndMillis = 0L,
        periodRangeStyle = StatsRangeStyle.DAYS,
        periodOptions = emptyList(),
        allStartMillis = 0L,
        allEndMillis = 0L,
        allRangeStyle = StatsRangeStyle.DAYS,
        totalMinor = amounts.sum(),
        isFirstDownload = false,
        isWaitingForInternet = false,
        expenseCount = amounts.count { it > 0 },
        pendingCount = 0,
        memberCount = 1,
        averagePerPersonMinor = amounts.sum(),
        myShareMinor = null,
        mySharePercent = 0,
        myPaidMinor = null,
        myPaidCount = 0,
        peopleView = StatsPeopleView.CONSUMPTION,
        people = emptyList(),
        chartType = StatsChartType.BARS,
        grain = StatsGrain.DAY,
        grains = emptyList(),
        bars = amounts.mapIndexed(::bar),
        chartTicksMinor = ticks,
        chartAverageMinor = null,
        grainNotes = emptyList(),
        biggestExpense = null,
        settling = null,
        categories = emptyList(),
        isLoaded = true,
        homeTab = HomeTab.GROUPS,
        activityUnreadCount = 0,
    )

    private companion object {
        const val DAY_MILLIS = 86_400_000L
    }
}
