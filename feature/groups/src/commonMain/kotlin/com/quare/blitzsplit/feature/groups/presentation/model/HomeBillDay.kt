package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: the bills of one day under "Hoje", "Ontem" or the date ("Qui, 29 de out"), which each UI writes.
data class HomeBillDay(
    // Why: the day's first instant in the device's time zone.
    val dayStartMillis: Long,
    val relativeDay: RelativeDay,
    // Why: the most recent first.
    val bills: List<HomeBill>,
)
