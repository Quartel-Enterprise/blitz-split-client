package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a new bill and an edited one are saved alike until they sync.
internal fun pendingExpenseOf(
    id: String,
    groupId: String,
    description: String,
    amountMinor: Long,
    currency: GroupCurrency,
    payers: List<ExpensePayer>,
    splitMode: SplitMode,
    shares: List<ExpenseShare>,
    items: BillItems?,
    categoryId: String?,
    spentAt: Long,
    now: Long,
): Expense = Expense(
    id = id,
    groupId = groupId,
    // Why: trimmed again after the cap, which may end on a space.
    description = ExpenseRules.toCapped(description.trim()).trimEnd(),
    amountMinor = amountMinor,
    currency = currency,
    splitMode = splitMode,
    payers = payers,
    shares = shares,
    items = items,
    categoryId = categoryId,
    spentAt = spentAt,
    isPending = true,
    updatedAt = now,
    updatedByMemberId = null,
)
