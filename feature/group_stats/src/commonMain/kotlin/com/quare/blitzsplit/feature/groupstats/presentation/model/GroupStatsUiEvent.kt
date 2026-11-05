package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "group_stats"

sealed interface GroupStatsUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            /*
             * Why: a tab of the home's rail, which each platform routes through whichever screen is in front: where it
             * went is its screen view.
             */
            is OnRailTabClicked -> EventAnalytics.NotTracked

            // Why: only a search that opens, never a late tap over what the statistics opened.
            OnRailSearchClicked -> EventAnalytics.Manual("search_opened")

            is OnPeriodPicked -> EventAnalytics.Automatic("stats_period_selected", mapOf("period" to period))

            is OnCustomPeriodPicked -> EventAnalytics.Automatic("stats_custom_period_selected")

            // Why: only another option, never the one showing, as Apple's Picker reports only a change.
            is OnPeopleViewPicked -> EventAnalytics.Manual("stats_people_view_selected")

            is OnChartTypePicked -> EventAnalytics.Manual("stats_chart_type_selected")

            // Why: only a grain the chart takes that changes it, or the period it switches to the whole one.
            is OnGrainPicked -> EventAnalytics.Manual("stats_grain_selected")

            is OnPersonClicked -> EventAnalytics.Automatic("person_clicked", mapOf("source" to SOURCE))

            OnBiggestExpenseClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to SOURCE))

            OnSplitBillClicked -> EventAnalytics.Automatic("split_bill_clicked", mapOf("source" to SOURCE))

            OnWholePeriodClicked -> EventAnalytics.Automatic("stats_whole_period_clicked")
        }

    data object OnBackClicked : GroupStatsUiEvent

    // Why: a tab of the home's rail, which Android keeps beside the statistics.
    data class OnRailTabClicked(
        val tab: HomeTab,
    ) : GroupStatsUiEvent

    // Why: the rail's last item, "Buscar", as on the home's rail: the search over the tab the group was opened from.
    data object OnRailSearchClicked : GroupStatsUiEvent

    // Why: an entry of the period menu; "Escolher datas…" opens each platform's date picker instead.
    data class OnPeriodPicked(
        val period: StatsPeriod,
    ) : GroupStatsUiEvent

    // Why: the days picked in "Escolher datas…", any instant of each, in the device's time zone.
    data class OnCustomPeriodPicked(
        val startMillis: Long,
        val endMillis: Long,
    ) : GroupStatsUiEvent

    // Why: "Consumo" or "Pagou".
    data class OnPeopleViewPicked(
        val view: StatsPeopleView,
    ) : GroupStatsUiEvent

    // Why: "Barras" or "Linha".
    data class OnChartTypePicked(
        val chartType: StatsChartType,
    ) : GroupStatsUiEvent

    // Why: "Dia", "Semana", "Mês" or "Ano".
    data class OnGrainPicked(
        val grain: StatsGrain,
    ) : GroupStatsUiEvent

    // Why: a person of "Por pessoa", which opens their profile.
    data class OnPersonClicked(
        val memberId: String,
    ) : GroupStatsUiEvent

    // Why: "Maior despesa", which opens the bill.
    data object OnBiggestExpenseClicked : GroupStatsUiEvent

    // Why: "Rachar conta" of a group without bills.
    data object OnSplitBillClicked : GroupStatsUiEvent

    // Why: "Ver toda a viagem" of a period without bills.
    data object OnWholePeriodClicked : GroupStatsUiEvent
}
