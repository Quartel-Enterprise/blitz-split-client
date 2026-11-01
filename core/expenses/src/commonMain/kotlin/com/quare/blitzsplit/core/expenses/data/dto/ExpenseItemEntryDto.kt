package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: one person in an item, with what was typed for them in its mode (BillItem.entries).
@Serializable
data class ExpenseItemEntryDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("value") val value: Long,
)
