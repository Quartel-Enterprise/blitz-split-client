package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: one row of "7 pagamentos → 5 pagamentos" in the confirmation, for the payments in one currency.
data class SimplifyCurrencyChange(
    val currency: GroupCurrency,
    val paymentsBefore: Int,
    val paymentsAfter: Int,
) {
    // Why: nothing to save in this currency (13.3c).
    val isAlreadyMinimal: Boolean
        get() = paymentsAfter >= paymentsBefore
}
