package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

fun interface ObserveSettlements {
    // Why: the group's payments, the most recent first.
    operator fun invoke(groupId: String): Flow<List<Settlement>>
}
