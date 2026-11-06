package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SetSimplifiesDebts
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsChange
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsPair
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first

/*
 * Why: the change carries the group's pairs right before and after, as this device has them, payments still in the
 * queue included (13.3d): Atividade and the push word from it what changed for each person (13.7).
 */
class SetSimplifiesDebtsUseCase(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SetSimplifiesDebts {
    override suspend fun invoke(
        groupId: String,
        simplifiesDebts: Boolean,
    ): Result<Unit> = suspendRunCatching {
        val group = checkNotNull(groupRepository.observeGroups().first().firstOrNull { it.id == groupId }) {
            "No group $groupId"
        }
        val expenses = expenseRepository.observeExpenses(groupId).first()
        val settlements = settlementRepository.observeSettlements(groupId).first()
        // Why: each currency simplifies on its own; their pairs are listed side by side, never added.
        val raws = GroupBalances
            .currenciesOf(group, expenses, settlements)
            .map { GroupBalances.createUnsimplified(expenses, settlements, it) }
        val simplified = raws.map { it.toSimplified(group.members.map { member -> member.id }) }
        val before = if (group.simplifiesDebts) simplified else raws
        val after = if (simplifiesDebts) simplified else raws
        groupRepository
            .updateSimplifiesDebts(
                groupId = groupId,
                simplifiesDebts = simplifiesDebts,
                change = SimplifyDebtsChange(
                    before = before.flatMap { it.debts }.map(::toPair),
                    after = after.flatMap { it.debts }.map(::toPair),
                ),
                changedAt = currentTimeMillis(),
            ).getOrThrow()
    }

    private fun toPair(debt: Debt): SimplifyDebtsPair = SimplifyDebtsPair(
        fromMemberId = debt.fromMemberId,
        toMemberId = debt.toMemberId,
        amountMinor = debt.amountMinor,
    )
}
