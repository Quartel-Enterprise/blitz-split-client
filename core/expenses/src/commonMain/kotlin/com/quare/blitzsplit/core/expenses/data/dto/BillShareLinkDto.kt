package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: a row of bill_share_links, which only the group's members read.
@Serializable
data class BillShareLinkDto(
    @SerialName("code") val code: String,
    @SerialName("visibility") val visibility: String,
)
