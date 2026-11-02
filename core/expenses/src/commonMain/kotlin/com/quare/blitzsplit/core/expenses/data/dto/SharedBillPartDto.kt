package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedBillPartDto(
    @SerialName("member_id") val memberId: String,
    // Why: null once the member left the group.
    @SerialName("name") val name: String?,
    @SerialName("amount_minor") val amountMinor: Long,
)
