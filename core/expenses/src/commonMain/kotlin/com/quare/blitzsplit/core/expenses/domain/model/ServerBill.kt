package com.quare.blitzsplit.core.expenses.domain.model

// Why: what the server says of one bill the user may see: its group, and whether it was deleted.
data class ServerBill(
    val groupId: String,
    val isDeleted: Boolean,
)
