package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroupBalances {
    // Why: who owes whom in each currency, again whenever a bill or a payment changes, here or through a sync.
    operator fun invoke(groupId: String): Flow<List<CurrencyBalances>>
}
