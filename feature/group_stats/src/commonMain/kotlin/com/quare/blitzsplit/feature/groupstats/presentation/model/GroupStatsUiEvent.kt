package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupStatsUiEvent : UiEvent {
    data object OnBackClicked : GroupStatsUiEvent

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

    // Why: "Maior despesa", which opens the bill.
    data object OnBiggestExpenseClicked : GroupStatsUiEvent

    // Why: "Rachar conta" of a group without bills.
    data object OnSplitBillClicked : GroupStatsUiEvent

    // Why: "Ver toda a viagem" of a period without bills.
    data object OnWholePeriodClicked : GroupStatsUiEvent
}
