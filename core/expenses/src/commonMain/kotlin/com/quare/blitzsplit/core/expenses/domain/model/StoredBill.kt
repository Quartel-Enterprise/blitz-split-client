package com.quare.blitzsplit.core.expenses.domain.model

// Why: one bill as the server or the device keeps it: its group, and whether it was deleted.
data class StoredBill(
    val groupId: String,
    val isDeleted: Boolean,
)
