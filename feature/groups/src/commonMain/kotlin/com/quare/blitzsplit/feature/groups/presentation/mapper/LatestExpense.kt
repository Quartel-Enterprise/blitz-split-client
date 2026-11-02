package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: one of Início's five latest bills before its category is read, so a category's change redoes only these.
internal data class LatestExpense(
    val group: Group,
    val expense: Expense,
    val myBalanceMinor: Long,
)
