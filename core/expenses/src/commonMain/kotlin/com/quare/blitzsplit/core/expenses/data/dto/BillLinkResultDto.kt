package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: what set_bill_link and disable_bill_link answer: "ok" with the link (set only), or "not_found".
@Serializable
data class BillLinkResultDto(
    @SerialName("status") val status: String,
    @SerialName("code") val code: String?,
    @SerialName("visibility") val visibility: String?,
)
