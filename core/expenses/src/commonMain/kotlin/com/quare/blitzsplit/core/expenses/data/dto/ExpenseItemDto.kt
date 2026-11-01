package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseItemDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("quantity") val quantity: Long,
    @SerialName("unit_price_minor") val unitPriceMinor: Long,
    // Why: the ItemSplitMode's code: "equal", "units", "shares", "amount" or "percent".
    @SerialName("mode") val mode: String,
    @SerialName("entries") val entries: List<ExpenseItemEntryDto>,
)
