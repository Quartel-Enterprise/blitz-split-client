package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillPayingMethodDto(
    @SerialName("user_id") val userId: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("payment_method_id") val methodId: String?,
    @SerialName("updated_at") val updatedAt: Long,
)
