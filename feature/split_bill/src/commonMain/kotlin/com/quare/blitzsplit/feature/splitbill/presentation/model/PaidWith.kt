package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod

/*
 * Why: "Paid with", which of their own payment methods this device's user paid the bill with. Private: it is saved for
 * them alone, never with the bill, so the group's other members never see it.
 */
data class PaidWith(
    // Why: the person's methods, the default first; none hides the row.
    val methods: List<PayingMethod>,
    // Why: the method picked, null for none.
    val methodId: String?,
    // Why: the mark as the form opened (a new bill's default, a saved bill's own), what a change is compared with.
    val openedMethodId: String?,
) {
    // Why: null for none, also once the method picked was deleted meanwhile.
    val method: PayingMethod?
        get() = methods.firstOrNull { it.id == methodId }

    val hasChanged: Boolean
        get() = method?.id != openedMethodId
}
