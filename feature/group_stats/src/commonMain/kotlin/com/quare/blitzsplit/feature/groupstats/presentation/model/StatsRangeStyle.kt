package com.quare.blitzsplit.feature.groupstats.presentation.model

/*
 * Why: how a UI writes a period's dates: one month ("setembro de 2026"), days ("24 a 29 de out") while it fits "Dia",
 * or months ("mar de 2024 a out de 2026") past that.
 */
enum class StatsRangeStyle {
    MONTH,
    DAYS,
    MONTHS,
}
