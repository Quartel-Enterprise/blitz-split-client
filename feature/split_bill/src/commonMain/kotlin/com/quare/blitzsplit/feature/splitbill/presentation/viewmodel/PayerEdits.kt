package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmountRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: "Editar conta" opens what each of several payers paid, whoever left the group among them as "Ex-membro".
internal fun payerAmountsOf(
    expense: Expense,
    people: List<BillPerson>,
    currency: GroupCurrency,
    decimalSeparator: Char,
): PayerAmounts {
    val paidById = expense.payers.groupBy { it.memberId }.mapValues { (_, payers) -> payers.sumOf { it.amountMinor } }
    val amountTextOf = { memberId: String ->
        paidById[memberId]
            ?.let {
                AmountInput.format(
                    amountMinor = it,
                    currency = currency,
                    decimalSeparator = decimalSeparator,
                )
            }.orEmpty()
    }
    val memberRows = people.map { person ->
        PayerAmountRow(
            memberId = person.id,
            person = person,
            amountText = amountTextOf(person.id),
        )
    }
    val memberIds = people.map { it.id }.toSet()
    val formerRows = paidById.keys.filterNot { it in memberIds }.map { memberId ->
        PayerAmountRow(
            memberId = memberId,
            person = null,
            amountText = amountTextOf(memberId),
        )
    }
    return PayerAmounts(
        rows = memberRows + formerRows,
        totalMinor = expense.amountMinor,
        currency = currency,
    )
}

/*
 * Why: "Várias pessoas pagaram" opens with whoever paid alone holding the whole total, so typing someone else's part
 * shows how much to take off theirs.
 */
internal fun SplitBillUiState.toPayerAmounts(decimalSeparator: Char): PayerAmounts {
    val total = amountMinor?.takeIf { isAmountValid } ?: 0L
    val paidText = if (total > 0) AmountInput.format(total, currency, decimalSeparator) else ""
    val memberRows = people.map { person ->
        PayerAmountRow(
            memberId = person.id,
            person = person,
            amountText = if (person.id == payerId) paidText else "",
        )
    }
    val formerRow = PayerAmountRow(
        memberId = payerId,
        person = null,
        amountText = paidText,
    ).takeIf { isPayerFormerMember }
    return PayerAmounts(
        rows = memberRows + listOfNotNull(formerRow),
        totalMinor = total,
        currency = currency,
    )
}

/*
 * Why: the people of the group as they are now, a new member with nothing paid. A bill being edited keeps what
 * someone who left paid, as "Ex-membro", so saving it never changes who paid unasked; a new bill drops them.
 */
internal fun PayerAmounts.withPeople(
    people: List<BillPerson>,
    currency: GroupCurrency,
    keepsFormer: Boolean,
): PayerAmounts {
    val byId = rows.associateBy { it.memberId }
    val memberRows = people.map { person ->
        byId[person.id]?.copy(person = person) ?: PayerAmountRow(
            memberId = person.id,
            person = person,
            amountText = "",
        )
    }
    val memberIds = people.map { it.id }.toSet()
    // Why: an "Ex-membro" row stays while its field is cleared, so it can be typed again; whoever leaves blank goes.
    val formerRows = rows
        .filter { keepsFormer && it.memberId !in memberIds && (it.person == null || it.amountText.isNotBlank()) }
        .map { it.copy(person = null) }
    return copy(
        rows = memberRows + formerRows,
        currency = currency,
    )
}

internal fun PayerAmounts.withAmountText(
    memberId: String,
    amountText: String,
): PayerAmounts = copy(
    rows = rows.map { row -> if (row.memberId == memberId) row.copy(amountText = amountText) else row },
)

// Why: who paid also decides who keeps the cents a split can't share evenly.
internal fun SplitBillUiState.withPayers(
    payerId: String,
    severalPayers: PayerAmounts?,
): SplitBillUiState {
    val paid = copy(
        payerId = payerId,
        severalPayers = severalPayers,
    )
    return paid.copy(
        split = split.copy(payerIds = paid.payerIds).withShares(),
        splitEditor = splitEditor?.copy(payerIds = paid.payerIds)?.withShares(),
    )
}

// Why: once the amounts add up, they are who paid; one person paying it all is a bill with one payer again.
internal fun SplitBillUiState.withSeveralPayersConfirmed(): SplitBillUiState {
    val amounts = payerAmounts?.takeIf { it.addsUp } ?: return this
    val onlyPayer = amounts.payers.singleOrNull()
    val paid = if (onlyPayer == null) {
        withPayers(
            payerId = "",
            severalPayers = amounts,
        )
    } else {
        withPayers(
            payerId = onlyPayer.memberId,
            severalPayers = null,
        )
    }
    return paid.copy(payerAmounts = null)
}

// Why: "Pronto" on "Quem pagou"; with "Várias pessoas" still marked it leaves the several payers as they were.
internal fun SplitBillUiState.withPayerConfirmed(): SplitBillUiState {
    val selection = payerSelection ?: return this
    val paid = selection.memberId?.let {
        withPayers(
            payerId = it,
            severalPayers = null,
        )
    } ?: this
    return paid.copy(payerSelection = null)
}

// Why: "Várias pessoas pagaram" takes the place of "Quem pagou", with what each one paid as last set.
internal fun SplitBillUiState.withSeveralPayersOpened(decimalSeparator: Char): SplitBillUiState = copy(
    payerSelection = null,
    payerAmounts = severalPayers ?: toPayerAmounts(decimalSeparator),
)
