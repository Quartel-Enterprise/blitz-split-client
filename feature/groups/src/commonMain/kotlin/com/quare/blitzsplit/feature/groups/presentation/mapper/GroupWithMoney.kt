package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.Expense
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
