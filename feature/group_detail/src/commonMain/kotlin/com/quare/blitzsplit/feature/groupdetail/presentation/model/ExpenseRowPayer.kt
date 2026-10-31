package com.quare.blitzsplit.feature.groupdetail.presentation.model

// Why: one of whoever paid a bill, as Despesas names them; each UI says "Você" for isMe.
data class ExpenseRowPayer(
    // Why: null for someone who paid and is no longer in the group, whom each UI calls "Ex-membro".
    val name: String?,
    val isMe: Boolean,
)
