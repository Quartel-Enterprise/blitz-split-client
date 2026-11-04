package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/*
 * Why: offline included, a bill or a payment saved on the device counts in the balances at once, and so does
 * "Simplificar dívidas" turned on or off; a group gone shows its pairs as the bills make them.
 */
class ObserveGroupBalancesUseCase(
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val groupRepository: GroupRepository,
) : ObserveGroupBalances {
    override fun invoke(groupId: String): Flow<GroupBalances> = combine(
        groupRepository
            .observeGroups()
            .map { groups -> groups.firstOrNull { it.id == groupId } }
            .distinctUntilChanged(),
        expenseRepository.observeExpenses(groupId),
        settlementRepository.observeSettlements(groupId),
    ) { group, expenses, settlements ->
        if (group == null) {
            GroupBalances.of(expenses, settlements)
        } else {
            GroupBalances.of(group, expenses, settlements)
        }
    }.distinctUntilChanged()
}
