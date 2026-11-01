package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: the service's percentage or the couvert per person, being typed until "Pronto"; empty is none.
data class FeeEditor(
    val fee: ItemsFee,
    val valueText: String,
    val currency: GroupCurrency,
) {
    // Why: the percentage or the amount in the minor unit, 0 when empty, null for what isn't one.
    val value: Long? = if (valueText.isBlank()) {
        0L
    } else {
        when (fee) {
            ItemsFee.SERVICE -> AmountInput.parseWhole(valueText)?.takeIf { it <= BillItemsSplit.MAX_SERVICE_PERCENT }

            ItemsFee.COUVERT ->
                AmountInput
                    .parse(valueText, currency)
                    ?.takeIf { it <= ExpenseRules.maxAmountMinorFor(currency) }
        }
    }

    val canSave: Boolean
        get() = value != null
}
