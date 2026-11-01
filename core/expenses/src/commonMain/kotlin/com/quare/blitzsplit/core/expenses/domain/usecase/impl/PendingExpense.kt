package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

// Why: a new bill and an edited one are saved alike until they sync.
internal fun pendingExpenseOf(
    id: String,
    groupId: String,
    description: String,
    amountMinor: Long,
    payers: List<ExpensePayer>,
    splitMode: SplitMode,
    shares: List<ExpenseShare>,
    items: BillItems?,
    spentAt: Long,
    now: Long,
): Expense = Expense(
    id = id,
    groupId = groupId,
    // Why: trimmed again after the cap, which may end on a space.
    description = ExpenseRules.toCapped(description.trim()).trimEnd(),
    amountMinor = amountMinor,
    splitMode = splitMode,
    payers = payers,
    shares = shares,
    items = items,
    spentAt = spentAt,
    isPending = true,
    updatedAt = now,
    updatedByMemberId = null,
)
