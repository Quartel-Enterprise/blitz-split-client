package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DayEntry
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

// Why: a bill of Despesas, in its own currency whichever the group's or the chip picked.
internal fun expenseRowOf(
    expense: Expense,
    namesById: Map<String, String>,
    meId: String?,
    payerOrder: PayerOrder,
    categoryTree: CategoryTree,
): DayEntry {
    val payers = payerOrder.sort(expense.payers.map { it.memberId }).map { id ->
        ExpenseRowPayer(
            name = namesById[id],
            isMe = id == meId,
        )
    }
    return ExpenseRow(
        id = expense.id,
        description = expense.description,
        spentAt = expense.spentAt,
        payers = payers,
        totalMinor = expense.amountMinor,
        currency = expense.currency,
        myBalanceMinor = meId?.let(expense::balanceOf),
        category = categoryTree.resolve(expense.categoryId),
        isPending = expense.isPending,
    )
}

// Why: a payment of Despesas, in its own currency.
internal fun paymentRowOf(
    settlement: Settlement,
    namesById: Map<String, String>,
    meId: String?,
    paidDay: RelativeDay,
): DayEntry = PaymentRow(
    id = settlement.id,
    paidAt = settlement.paidAt,
    paidDay = paidDay,
    fromName = namesById[settlement.fromMemberId],
    fromIsMe = settlement.fromMemberId == meId,
    toName = namesById[settlement.toMemberId],
    toIsMe = settlement.toMemberId == meId,
    amountMinor = settlement.amountMinor,
    currency = settlement.currency,
    method = settlement.method,
    isPending = settlement.isPending,
)
