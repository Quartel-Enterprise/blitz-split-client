package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: the items of one day, under "Hoje", "Ontem" or the date ("Sexta, 30 de outubro"), which each UI writes.
data class ActivityDay(
    // Why: the day's first instant in the device's time zone.
    val dayStartMillis: Long,
    val relativeDay: RelativeDay,
    val items: List<ActivityItem>,
)
