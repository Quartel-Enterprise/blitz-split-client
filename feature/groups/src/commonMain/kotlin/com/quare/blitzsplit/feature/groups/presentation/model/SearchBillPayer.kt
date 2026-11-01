package com.quare.blitzsplit.feature.groups.presentation.model

// Why: one of whoever paid a bill the search found; each UI says "Você" for isMe.
data class SearchBillPayer(
    // Why: null for someone who paid and is no longer in the group, whom each UI calls "Ex-membro".
    val name: String?,
    val isMe: Boolean,
)
