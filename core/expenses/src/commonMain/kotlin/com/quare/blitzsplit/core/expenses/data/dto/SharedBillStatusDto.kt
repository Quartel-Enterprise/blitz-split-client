package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: what comment_on_shared_bill answers: "ok", "not_found", "members_only" or "needs_name".
@Serializable
data class SharedBillStatusDto(
    @SerialName("status") val status: String,
)
