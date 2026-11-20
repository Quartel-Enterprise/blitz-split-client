package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BalanceSnapshot
import kotlinx.coroutines.flow.Flow

fun interface ObserveBalanceSnapshot {
    // Why: again whenever a group, a bill or a payment changes here or through a sync, so the widget follows.
    operator fun invoke(): Flow<BalanceSnapshot>
}
