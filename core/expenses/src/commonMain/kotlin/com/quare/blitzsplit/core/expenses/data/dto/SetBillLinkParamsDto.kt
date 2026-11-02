package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetBillLinkParamsDto(
    @SerialName("p_expense_id") val expenseId: String,
    @SerialName("p_visibility") val visibility: String,
)
