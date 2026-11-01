package com.quare.blitzsplit.feature.groupstats.presentation.model

/*
 * Why: the periods of the statistics, which only this screen keeps. ALL is "Toda a viagem" in a trip and "Todo o
 * período" in any other group, from the first to the last day with a bill; CUSTOM is "Escolher datas…".
 */
enum class StatsPeriod {
    ALL,
    THIS_YEAR,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM,
}
