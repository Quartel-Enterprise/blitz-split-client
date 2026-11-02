package com.quare.blitzsplit.android.feature.groupstats

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBiggestExpense
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainNote
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriodOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import kotlin.time.Duration.Companion.days

// Why: Viagem Floripa as the canvas draws it, 24 to 29 October 2026.
private const val PREVIEW_START = 1_793_329_200_000L
private val previewDay = 1.days.inWholeMilliseconds
private val previewDays = listOf(56_100L, 0L, 0L, 0L, 145_800L, 39_960L)
private val previewPeople = listOf(
    Triple("Bruno", AvatarColor.PEACH, 48_110L),
    Triple("Pierre", AvatarColor.BLUE, 47_110L),
    Triple("Ana", AvatarColor.GREEN, 46_310L),
    Triple("Carla", AvatarColor.LILAC, 41_990L),
    Triple("Elisa", AvatarColor.SAND, 32_990L),
    Triple("Diego", AvatarColor.GREEN, 25_350L),
)

internal fun createPreviewGroupStatsState(hasBills: Boolean = true): GroupStatsUiState {
    val end = PREVIEW_START + previewDay * (previewDays.size - 1)
    return GroupStatsUiState(
        groupName = "Viagem Floripa",
        currency = GroupCurrency.BRL,
        isTrip = true,
        period = StatsPeriod.ALL,
        periodStartMillis = PREVIEW_START,
        periodEndMillis = end,
        periodRangeStyle = StatsRangeStyle.DAYS,
        periodOptions = listOf(
            StatsPeriodOption(StatsPeriod.ALL, PREVIEW_START, end, StatsRangeStyle.DAYS, 7),
            StatsPeriodOption(StatsPeriod.CUSTOM, null, null, StatsRangeStyle.DAYS, 0),
        ),
        allStartMillis = PREVIEW_START.takeIf { hasBills },
        allEndMillis = end.takeIf { hasBills },
        allRangeStyle = StatsRangeStyle.DAYS,
        totalMinor = if (hasBills) 241_860L else 0L,
        expenseCount = if (hasBills) 7 else 0,
        pendingCount = if (hasBills) 1 else 0,
        memberCount = 6,
        averagePerPersonMinor = 40_310L,
        myShareMinor = 47_110L,
        mySharePercent = 19,
        myPaidMinor = 141_600L,
        myPaidCount = 3,
        peopleView = StatsPeopleView.CONSUMPTION,
        people = previewPeople.mapIndexed { index, (name, color, amount) ->
            StatsPersonRow(
                memberId = "member-$index",
                name = name,
                initials = name.take(2).uppercase(),
                avatarColor = color,
                isMe = name == "Pierre",
                amountMinor = amount,
                barFraction = amount.toDouble() / previewPeople.first().third,
            )
        },
        chartType = StatsChartType.BARS,
        grain = StatsGrain.DAY,
        grains = StatsGrain.entries.map {
            StatsGrainOption(
                it,
                isEnabled = it == StatsGrain.DAY || it == StatsGrain.WEEK,
            )
        },
        bars = previewDays.mapIndexed { index, amount ->
            StatsBar(
                startMillis = PREVIEW_START + previewDay * index,
                endMillis = PREVIEW_START + previewDay * index,
                amountMinor = amount,
                isToday = index == previewDays.lastIndex,
                isPeak = index == 4,
                showsLabel = true,
                showsDot = true,
            )
        },
        grainNotes = listOf(StatsGrainNote.MONTH_AND_YEAR_NEED_LONGER_PERIOD),
        biggestExpense = StatsBiggestExpense(
            expenseId = "boat",
            description = "Passeio de barco",
            spentAt = PREVIEW_START + previewDay * 4 + previewDay / 2,
            payers = listOf(StatsPayer("Pierre", isMe = true)),
            amountMinor = 72_000L,
            isPending = false,
        ),
        isLoaded = true,
        homeTab = HomeTab.GROUPS,
        activityUnreadCount = 0,
    )
}
