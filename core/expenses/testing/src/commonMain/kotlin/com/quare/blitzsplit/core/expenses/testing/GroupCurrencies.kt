package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: a scenario's bills are written once and the group's currency is what decides theirs, as the app saves them.
fun List<Expense>.inCurrencyOf(groups: List<Group>): List<Expense> = map { expense ->
    groups.firstOrNull { it.id == expense.groupId }?.let { expense.copy(currency = it.currency) } ?: expense
}
