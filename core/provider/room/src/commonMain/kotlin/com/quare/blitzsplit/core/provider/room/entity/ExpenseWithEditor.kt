package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Embedded

// Why: a bill with the member who last saved it, found through the account the server recorded.
data class ExpenseWithEditor(
    @Embedded
    val expense: ExpenseEntity,
    // Why: null while nobody synced a change to it, or when that account is no member of the group.
    val updatedByMemberId: String?,
)
