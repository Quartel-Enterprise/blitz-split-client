package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

/*
 * Why: the currency picked beside "Valor". The typed numbers keep their digits as the group's people are shown again
 * (GroupEdits.withGroupShown); Dividir closes, and a split by items starts over, equally among everyone (a new bill
 * then follows the group's "Divisão padrão" again), since its prices were in the old currency's minor unit.
 */
internal fun SplitBillUiState.withCurrencyPicked(
    currency: GroupCurrency,
    decimalSeparator: Char,
): SplitBillUiState {
    val split = if (split.mode == SplitMode.ITEMS) {
        createEmptySplit(this.currency, showsSats)
            .withPeople(people, this.currency, showsSats, split.payerIds, includesNewcomers = true, decimalSeparator)
    } else {
        split
    }
    return copy(
        pickedCurrency = currency,
        splitEditor = null,
        split = split,
    )
}

// Why: "Valor" as typed, filtered to the bill's currency, and the split and the payers adding up to it.
internal fun SplitBillUiState.withAmountTyped(
    text: String,
    amountAnchor: Pair<Long, List<SplitEntry>>?,
    decimalSeparator: Char,
): SplitBillUiState = copy(
    amountText = TypedText.toAmountText(amountText, text, currency, showsSats, decimalSeparator),
    hasEditedAmount = true,
).withBillTotal(amountAnchor, decimalSeparator)

// Why: the parts of "Valores" as they are, which a new total rescales from; none unless they add up.
internal fun SplitBillUiState.valoresAnchorOrNull(): Pair<Long, List<SplitEntry>>? =
    split.takeIf { it.mode == SplitMode.AMOUNT && it.addsUp && it.totalMinor > 0 }?.let { it.totalMinor to it.entries }
