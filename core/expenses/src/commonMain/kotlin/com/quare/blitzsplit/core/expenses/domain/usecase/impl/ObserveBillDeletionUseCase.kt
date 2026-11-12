package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillDeletion
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveBillDeletionUseCase(
    private val groups: GroupRepository,
    private val expenses: ExpenseRepository,
    private val settlements: SettlementRepository,
) : ObserveBillDeletion {
    override fun invoke(
        groupId: String,
        expenseId: String,
    ): Flow<BillDeletion?> = combine(
        groups.observeGroups(),
        expenses.observeExpenses(groupId),
        settlements.observeSettlements(groupId),
    ) { allGroups, groupExpenses, groupSettlements ->
        val expense = groupExpenses.firstOrNull { it.id == expenseId } ?: return@combine null
        val group = allGroups.firstOrNull { it.id == groupId }
        BillDeletion.of(
            expense = expense,
            myMemberId = group?.members?.firstOrNull { it.isMe }?.id,
            settledPair = SettledPairChange.findDeletionOrNull(expenseId, group, groupExpenses, groupSettlements),
        )
    }.distinctUntilChanged()
}
