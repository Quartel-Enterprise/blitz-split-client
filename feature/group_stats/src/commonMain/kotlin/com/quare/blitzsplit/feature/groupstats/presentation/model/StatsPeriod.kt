package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.ExpensePeriod

/*
 * Why: the periods of the statistics, which only this screen keeps. ALL is "Toda a viagem" in a trip and "Todo o
 * período" in any other group, from the first to the last day with a bill; CUSTOM is "Escolher datas…".
 */
enum class StatsPeriod(
    // Why: the same period in All expenses, which a category opens with and which counts its days the same way.
    val expensePeriod: ExpensePeriod,
) {
    ALL(ExpensePeriod.ALL),
    THIS_YEAR(ExpensePeriod.THIS_YEAR),
    THIS_MONTH(ExpensePeriod.THIS_MONTH),
    LAST_MONTH(ExpensePeriod.LAST_MONTH),
    CUSTOM(ExpensePeriod.CUSTOM),
}
