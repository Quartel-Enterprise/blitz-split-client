package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: the bills of one day, under "Today", "Yesterday" or the date, which each UI writes as the group's Expenses.
data class CategoryExpenseDay(
    // Why: the day's first instant in the device's time zone.
    val dayStartMillis: Long,
    val relativeDay: RelativeDay,
    // Why: the most recent first.
    val expenses: List<CategoryExpenseRow>,
)
