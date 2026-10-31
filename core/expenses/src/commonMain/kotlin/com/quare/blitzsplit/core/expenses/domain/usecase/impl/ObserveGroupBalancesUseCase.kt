package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Why: offline included, a bill or a payment saved on the device counts in the balances at once.
class ObserveGroupBalancesUseCase(
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
) : ObserveGroupBalances {
    override fun invoke(groupId: String): Flow<GroupBalances> = combine(
        expenseRepository.observeExpenses(groupId),
        settlementRepository.observeSettlements(groupId),
        GroupBalances::of,
    ).distinctUntilChanged()
}
