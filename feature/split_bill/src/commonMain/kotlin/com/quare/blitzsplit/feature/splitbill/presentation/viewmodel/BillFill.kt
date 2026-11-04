package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: "Editar conta" opens filled in with the bill as it was saved, its split as it was typed.
internal fun SplitBillUiState.toFilledWith(
    group: Group,
    expense: Expense,
    decimalSeparator: Char,
    spentDay: RelativeDay,
): SplitBillUiState {
    val people = group.members.map { it.toBillPerson() }
    val payerId = expense.payers
        .singleOrNull()
        ?.memberId
        .orEmpty()
    val severalPayers = expense.payers.takeIf { it.size > 1 }?.let {
        payerAmountsOf(
            expense = expense,
            people = people,
            currency = group.currency,
            showsSats = showsSats,
            decimalSeparator = decimalSeparator,
        )
    }
    return copy(
        currency = group.currency,
        amountText = AmountInput.format(
            amountMinor = expense.amountMinor,
            currency = group.currency,
            showsSats = showsSats,
            decimalSeparator = decimalSeparator,
        ),
        description = expense.description,
        categoryId = expense.categoryId,
        isCategoryPicked = true,
        payerId = payerId,
        severalPayers = severalPayers,
        spentAt = expense.spentAt,
        spentDay = spentDay,
        split = splitEditorOf(
            expense = expense,
            people = people,
            currency = group.currency,
            showsSats = showsSats,
            decimalSeparator = decimalSeparator,
        ),
    )
}

// Why: someone else's save that a sync brought, while the form keeps what is being typed here.
internal fun SplitBillUiState.withChangeNoticed(
    group: Group,
    previous: Expense,
    expense: Expense,
): SplitBillUiState {
    if (expense.updatedAt == previous.updatedAt || expense.isPending) return this
    val changer = group.members.firstOrNull { it.id == expense.updatedByMemberId }
    return copy(
        conflict = BillConflict(
            changerName = changer?.name,
            changerIsMe = changer?.isMe == true,
            changedAt = expense.updatedAt,
        ),
    )
}
