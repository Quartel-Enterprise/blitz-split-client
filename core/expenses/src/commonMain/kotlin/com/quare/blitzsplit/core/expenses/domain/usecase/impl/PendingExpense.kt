package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

// Why: a new bill and an edited one are saved alike, one person paying it all, until they sync.
internal fun pendingExpenseOf(
    id: String,
    groupId: String,
    description: String,
    amountMinor: Long,
    payerId: String,
    splitMode: SplitMode,
    shares: List<ExpenseShare>,
    spentAt: Long,
    now: Long,
): Expense = Expense(
    id = id,
    groupId = groupId,
    // Why: trimmed again after the cap, which may end on a space.
    description = ExpenseRules.toCapped(description.trim()).trimEnd(),
    amountMinor = amountMinor,
    splitMode = splitMode,
    payers = listOf(
        ExpensePayer(
            memberId = payerId,
            amountMinor = amountMinor,
        ),
    ),
    shares = shares,
    spentAt = spentAt,
    isPending = true,
    updatedAt = now,
    updatedByMemberId = null,
)
