package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

// Why: "Hoje", "Ontem" or the date over its lines, the most recent first.
data class BillHistoryDay(
    val dayStartMillis: Long,
    val relativeDay: RelativeDay,
    val entries: List<BillHistoryEntry>,
)
