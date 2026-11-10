package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.feature.groupstats.GroupStatsContent
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBiggestExpense
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainNote
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriodOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import kotlin.math.roundToInt

/** Statistics of the trip in its own currency: the days, who consumed what and where the money went. */
@Composable
internal fun StatisticsScreenshotContent(trip: SampleTrip) {
    GroupStatsContent(
        state = trip.groupStatsState(),
        onEvent = {},
        onPickDates = {},
    )
}

private fun SampleTrip.groupStatsState(): GroupStatsUiState {
    val bills = bills.filter { it.currency == currency }
    val firstDay = bills.maxOf { it.daysAgo }
    val start = startOfDayOf(firstDay)
    val end = startOfDayOf(0)
    val total = bills.sumOf { it.totalMinor }
    val consumed = people.indices
        .associateWith { person -> bills.sumOf { sharesOf(it)[person] ?: 0L } }
        .entries
        .sortedByDescending { it.value }
    val days = (firstDay downTo 0).map { daysAgo -> daysAgo to bills.filter { it.daysAgo == daysAgo } }
    val peak = days.maxBy { (_, dayBills) -> dayBills.sumOf { it.totalMinor } }.first
    val biggest = bills.maxBy { it.totalMinor }
    val categories = bills
        .groupBy { bill -> categoryOf(bill.categoryKey).let { category -> category.parentId ?: category.id } }
        .map { (key, categoryBills) -> categoryOf(key) to categoryBills.sumOf { it.totalMinor } }
        .sortedByDescending { it.second }
    val dailyMax = days.maxOf { (_, dayBills) -> dayBills.sumOf { it.totalMinor } }
    val myShare = consumed.first { it.key == me }.value
    return GroupStatsUiState(
        groupName = groupName,
        currency = currency,
        currencies = listOf(currency, foreignCurrency),
        isTrip = true,
        period = StatsPeriod.ALL,
        periodStartMillis = start,
        periodEndMillis = end,
        periodRangeStyle = StatsRangeStyle.DAYS,
        periodOptions = listOf(
            StatsPeriodOption(StatsPeriod.ALL, start, end, StatsRangeStyle.DAYS, bills.size),
            StatsPeriodOption(StatsPeriod.CUSTOM, null, null, StatsRangeStyle.DAYS, 0),
        ),
        allStartMillis = start,
        allEndMillis = end,
        allRangeStyle = StatsRangeStyle.DAYS,
        totalMinor = total,
        isFirstDownload = false,
        isWaitingForInternet = false,
        expenseCount = bills.size,
        pendingCount = 0,
        memberCount = people.size,
        averagePerPersonMinor = total / people.size,
        myShareMinor = myShare,
        mySharePercent = (myShare * PERCENT / total).toInt(),
        myPaidMinor = bills.sumOf { bill -> bill.payers.filter { it.person == me }.sumOf { it.amountMinor } },
        myPaidCount = bills.count { bill -> bill.payers.any { it.person == me } },
        peopleView = StatsPeopleView.CONSUMPTION,
        people = consumed.map { (person, amount) ->
            StatsPersonRow(
                memberId = memberIdOf(person),
                name = people[person],
                initials = initialsOf(person),
                avatarColor = avatarColorOf(person),
                photoUrl = photoUrlOf(person),
                isMe = person == me,
                amountMinor = amount,
                barFraction = amount.toDouble() / consumed.first().value,
            )
        },
        chartType = StatsChartType.BARS,
        grain = StatsGrain.DAY,
        grains = StatsGrain.entries.map { StatsGrainOption(it, isEnabled = it == StatsGrain.DAY || it == StatsGrain.WEEK) },
        bars = days.map { (daysAgo, dayBills) ->
            val amount = dayBills.sumOf { it.totalMinor }
            StatsBar(
                startMillis = startOfDayOf(daysAgo),
                endMillis = startOfDayOf(daysAgo),
                amountMinor = amount,
                expenseCount = dayBills.size,
                isToday = daysAgo == 0,
                isPeak = daysAgo == peak,
                showsLabel = true,
                showsDot = true,
            )
        },
        chartTicksMinor = (0..TICKS).map { dailyMax * it / TICKS },
        chartAverageMinor = total / days.size,
        grainNotes = listOf(StatsGrainNote.MONTH_AND_YEAR_NEED_LONGER_PERIOD),
        biggestExpense = StatsBiggestExpense(
            expenseId = "biggest",
            description = biggest.description,
            spentAt = momentOf(biggest.daysAgo, biggest.minuteOfDay),
            payers = biggest.payers.map { StatsPayer(people[it.person], isMe = it.person == me) },
            amountMinor = biggest.totalMinor,
            isPending = false,
        ),
        settling = null,
        categories = categories.map { (category, amount) ->
            StatsCategoryRow(
                category = category,
                amountMinor = amount,
                percent = (amount * PERCENT.toDouble() / total).roundToInt(),
                barFraction = amount.toDouble() / categories.first().second,
            )
        },
        isLoaded = true,
        homeTab = HomeTab.GROUPS,
    )
}

private const val PERCENT = 100L
private const val TICKS = 3L
