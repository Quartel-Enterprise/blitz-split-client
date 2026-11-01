package com.quare.blitzsplit.feature.groupstats.presentation.model

// Why: an entry of the period menu, with its dates and how many bills it holds ("24 a 29 de out · 7 despesas").
data class StatsPeriodOption(
    val period: StatsPeriod,
    // Why: the first instant of its first and last days; null for "Escolher datas…" before any was picked.
    val startMillis: Long?,
    val endMillis: Long?,
    val rangeStyle: StatsRangeStyle,
    val expenseCount: Int,
)
