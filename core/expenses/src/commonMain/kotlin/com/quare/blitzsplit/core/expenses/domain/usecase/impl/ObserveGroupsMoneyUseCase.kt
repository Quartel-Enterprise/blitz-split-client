package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveGroupsMoneyUseCase(
    private val expenses: ExpenseRepository,
    private val settlements: SettlementRepository,
) : ObserveGroupsMoney {
    override fun invoke(): Flow<Map<String, GroupMoney>> = combine(
        expenses.observeAllExpenses(),
        settlements.observeAllSettlements(),
    ) { allExpenses, allSettlements ->
        val expensesByGroup = allExpenses.groupBy { it.groupId }
        val settlementsByGroup = allSettlements.groupBy { it.groupId }
        (expensesByGroup.keys + settlementsByGroup.keys).associateWith { groupId ->
            GroupMoney(
                expenses = expensesByGroup[groupId].orEmpty(),
                settlements = settlementsByGroup[groupId].orEmpty(),
            )
        }
    }
}
