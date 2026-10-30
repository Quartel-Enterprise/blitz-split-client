package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpensePayerDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("amount_minor") val amountMinor: Long,
)
