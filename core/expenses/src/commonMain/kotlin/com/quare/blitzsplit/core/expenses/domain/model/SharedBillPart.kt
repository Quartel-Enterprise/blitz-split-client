package com.quare.blitzsplit.core.expenses.domain.model

// Why: what one person paid of a shared bill, or what their part of it is.
data class SharedBillPart(
    val memberId: String,
    // Why: null once they left the group ("Ex-membro").
    val name: String?,
    val amountMinor: Long,
)
