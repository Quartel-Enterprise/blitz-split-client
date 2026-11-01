package com.quare.blitzsplit.android.feature.groupstats

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainNote
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriodOption

// Why: "Toda a viagem" in a trip, "Todo o período" in any other group.
@Composable
internal fun periodLabelOf(
    period: StatsPeriod,
    isTrip: Boolean,
): String = stringResource(
    when (period) {
        StatsPeriod.ALL -> if (isTrip) R.string.group_stats_period_all_trip else R.string.group_stats_period_all
        StatsPeriod.THIS_YEAR -> R.string.group_stats_period_this_year
        StatsPeriod.THIS_MONTH -> R.string.group_stats_period_this_month
        StatsPeriod.LAST_MONTH -> R.string.group_stats_period_last_month
        StatsPeriod.CUSTOM -> R.string.group_stats_period_custom
    },
)

// Why: "nenhuma despesa", "1 despesa" or "7 despesas".
@Composable
internal fun expenseCountTextOf(count: Int): String = if (count == 0) {
    stringResource(R.string.group_stats_expenses_none)
} else {
    pluralStringResource(R.plurals.group_stats_expenses, count, count)
}

// Why: under a period of the menu, "24–29 de out. · 7 despesas", or "De um dia até outro" before any dates.
@Composable
internal fun periodHintOf(
    option: StatsPeriodOption,
    dates: StatsTextFormatter,
): String {
    val start = option.startMillis
    val end = option.endMillis
    if (start == null || end == null) return stringResource(R.string.group_stats_period_custom_hint)
    return stringResource(
        R.string.group_stats_period_hint,
        dates.toRangeText(option.rangeStyle, start, end),
        expenseCountTextOf(option.expenseCount),
    )
}

@StringRes
internal fun grainLabelOf(grain: StatsGrain): Int = when (grain) {
    StatsGrain.DAY -> R.string.group_stats_grain_day
    StatsGrain.WEEK -> R.string.group_stats_grain_week
    StatsGrain.MONTH -> R.string.group_stats_grain_month
    StatsGrain.YEAR -> R.string.group_stats_grain_year
}

// Why: "Dia mais caro: 28 de out.,", before the amount.
@Composable
internal fun peakLabelOf(
    state: GroupStatsUiState,
    dates: StatsTextFormatter,
): String? {
    val peak = state.peak ?: return null
    val label = when (state.grain) {
        StatsGrain.DAY -> R.string.group_stats_peak_day
        StatsGrain.WEEK -> R.string.group_stats_peak_week
        StatsGrain.MONTH -> R.string.group_stats_peak_month
        StatsGrain.YEAR -> R.string.group_stats_peak_year
    }
    return stringResource(label, dates.toBarName(peak, state.grain))
}

// Why: the chart's whole text for TalkBack: "Gastos por dia: 24 de out. R$ 561,00; 25 de out. nada; …".
@Composable
internal fun chartDescriptionOf(
    state: GroupStatsUiState,
    dates: StatsTextFormatter,
    money: MoneyFormatter,
): String {
    val nothing = stringResource(R.string.group_stats_aria_nothing)
    val values = state.bars.joinToString("; ") { bar ->
        val amount = if (bar.amountMinor > 0) money.toText(bar.amountMinor) else nothing
        "${dates.toBarName(bar, state.grain)} $amount"
    }
    val label = when (state.grain) {
        StatsGrain.DAY -> R.string.group_stats_aria_day
        StatsGrain.WEEK -> R.string.group_stats_aria_week
        StatsGrain.MONTH -> R.string.group_stats_aria_month
        StatsGrain.YEAR -> R.string.group_stats_aria_year
    }
    return stringResource(label, values)
}

// Why: why some grains are off, one sentence per reason.
@Composable
internal fun grainNoteTextOf(state: GroupStatsUiState): String? = state.grainNotes
    .map { note ->
        stringResource(
            when (note) {
                StatsGrainNote.DAY_NEEDS_SHORT_PERIOD -> R.string.group_stats_note_day

                StatsGrainNote.WEEK_MONTH_AND_YEAR_NEED_LONGER_PERIOD -> R.string.group_stats_note_week_month_year

                StatsGrainNote.MONTH_AND_YEAR_NEED_LONGER_PERIOD -> R.string.group_stats_note_month_year

                StatsGrainNote.YEAR_NEEDS_LONGER_PERIOD -> R.string.group_stats_note_year

                StatsGrainNote.YEAR_SWITCHES_PERIOD -> if (state.isTrip) {
                    R.string.group_stats_note_year_switches_trip
                } else {
                    R.string.group_stats_note_year_switches_all
                }
            },
        )
    }.joinToString(" ")
    .ifEmpty { null }

// Why: "Nenhuma despesa em setembro" (or "em 2026"), "nesse período" for picked dates or the whole period.
@Composable
internal fun periodEmptyTitleOf(
    state: GroupStatsUiState,
    dates: StatsTextFormatter,
): String = when (state.period) {
    StatsPeriod.THIS_MONTH, StatsPeriod.LAST_MONTH ->
        stringResource(R.string.group_stats_period_empty_title, dates.toMonthText(state.periodStartMillis))

    StatsPeriod.THIS_YEAR ->
        stringResource(R.string.group_stats_period_empty_title, dates.toYearText(state.periodStartMillis))

    else -> stringResource(R.string.group_stats_period_empty_title_range)
}
