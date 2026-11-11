package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.datetime.TimeZone

// Why: "Editar conta" opens filled in with the bill as it was saved, its split as it was typed.
internal fun SplitBillUiState.toFilledWith(
    group: Group,
    expense: Expense,
    decimalSeparator: Char,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    // Why: the bill's own currency, never the group's, which is only the default of a new bill.
    val currency = expense.currency
    val people = group.members.map { it.toBillPerson() }
    val payerId = expense.payers
        .singleOrNull()
        ?.memberId
        .orEmpty()
    val severalPayers = expense.payers.takeIf { it.size > 1 }?.let {
        payerAmountsOf(
            expense = expense,
            people = people,
            currency = currency,
            showsSats = showsSats,
            decimalSeparator = decimalSeparator,
        )
    }
    return copy(
        currency = currency,
        pickedCurrency = currency,
        amountText = AmountInput.format(
            amountMinor = expense.amountMinor,
            currency = currency,
            showsSats = showsSats,
            decimalSeparator = decimalSeparator,
        ),
        description = expense.description,
        categoryId = expense.categoryId,
        isCategoryPicked = true,
        payerId = payerId,
        severalPayers = severalPayers,
        spentAt = expense.spentAt,
        spentDay = SpentDay.relativeDayOf(expense.spentAt, now, timeZone),
        hasSpentTime = SpentDay.hasTime(expense.spentAt, timeZone),
        split = splitEditorOf(
            expense = expense,
            people = people,
            currency = currency,
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
