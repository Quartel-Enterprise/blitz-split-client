package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.datetime.TimeZone

// Why: a later time today, which the pickers can't rule out, becomes now, the latest time they reach.
internal fun SplitBillUiState.withSpentAt(
    epochMillis: Long,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val spentAt = minOf(epochMillis, now)
    return copy(
        spentAt = spentAt,
        spentDay = SpentDay.relativeDayOf(
            epochMillis = spentAt,
            nowMillis = now,
            timeZone = timeZone,
        ),
        latestSpentAt = now,
    )
}
