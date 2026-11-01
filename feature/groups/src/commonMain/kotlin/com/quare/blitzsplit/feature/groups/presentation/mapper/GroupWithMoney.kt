package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: a group with what moves its balance on the home, as Room has it now.
internal data class GroupWithMoney(
    val group: Group,
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
    // Why: bills and payments deleted here and not on the server yet, which no list shows but still wait.
    val pendingDeletionCount: Int,
)

// Why: each group with its bills and payments by group id, none for a group with nothing on the device yet.
internal fun groupsWithMoneyOf(
    groups: List<Group>,
    money: Map<String, GroupMoney>,
): List<GroupWithMoney> = groups.map { group ->
    val ofGroup = money[group.id] ?: GroupMoney.NONE
    GroupWithMoney(
        group = group,
        expenses = ofGroup.expenses,
        settlements = ofGroup.settlements,
        pendingDeletionCount = ofGroup.pendingDeletionCount,
    )
}
