package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: the bills and payments of one day, under "Hoje", "Ontem" or the date ("Sex, 24 de out"), which each UI writes.
data class ExpenseDay(
    // Why: the day's first instant in the device's time zone.
    val dayStartMillis: Long,
    val relativeDay: RelativeDay,
    // Why: the most recent first, bills and payments mixed by their time.
    val entries: List<DayEntry>,
)
