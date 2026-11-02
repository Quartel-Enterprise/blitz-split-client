package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedBillExpenseDto(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("spent_at") val spentAt: Long,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("split_mode") val splitMode: String,
    @SerialName("payers") val payers: List<SharedBillPartDto>?,
    @SerialName("shares") val shares: List<SharedBillPartDto>?,
)
