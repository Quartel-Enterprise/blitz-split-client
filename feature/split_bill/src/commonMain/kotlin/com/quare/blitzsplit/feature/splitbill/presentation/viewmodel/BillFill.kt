package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
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
            decimalSeparator = decimalSeparator,
        )
    }
    return copy(
        currency = group.currency,
        amountText = AmountInput.format(
            amountMinor = expense.amountMinor,
            currency = group.currency,
            decimalSeparator = decimalSeparator,
        ),
        description = expense.description,
        payerId = payerId,
        severalPayers = severalPayers,
        spentAt = expense.spentAt,
        spentDay = spentDay,
        split = splitEditorOf(
            expense = expense,
            people = people,
            currency = group.currency,
            decimalSeparator = decimalSeparator,
        ),
    )
}
