package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import kotlinx.coroutines.flow.Flow

fun interface ObservePeopleBalances {
    // Why: again whenever a group, a bill or a payment changes here or through a sync.
    operator fun invoke(): Flow<PeopleBalances>
}
