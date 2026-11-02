package com.quare.blitzsplit.core.expenses.domain.model

// Why: a bill's link ("Compartilhar conta", 7.3), blitzsplit.com/c/<code>, and who may open it.
data class BillShareLink(
    val code: String,
    // Why: null for a way a newer app added, which this one shows unpicked rather than overwrites.
    val visibility: BillLinkVisibility?,
) {
    val url: String
        get() = SharedBillLink.of(code)
}
