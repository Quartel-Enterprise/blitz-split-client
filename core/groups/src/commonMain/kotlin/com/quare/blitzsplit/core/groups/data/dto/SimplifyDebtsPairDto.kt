package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimplifyDebtsPairDto(
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("amount") val amount: Long,
)
