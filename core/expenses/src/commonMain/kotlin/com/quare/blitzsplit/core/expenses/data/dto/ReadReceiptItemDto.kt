package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: one line of the receipt read, its price of each in the currency's minor unit.
@Serializable
data class ReadReceiptItemDto(
    @SerialName("name") val name: String,
    @SerialName("quantity") val quantity: Long,
    @SerialName("unitPriceMinor") val unitPriceMinor: Long,
)
