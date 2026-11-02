package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: what read-receipt read, amounts in the currency's minor unit, the date "YYYY-MM-DD" and the time "HH:MM".
@Serializable
data class ReadReceiptResponseDto(
    @SerialName("totalMinor") val totalMinor: Long,
    @SerialName("description") val description: String?,
    @SerialName("category") val category: String?,
    @SerialName("date") val date: String?,
    @SerialName("time") val time: String?,
    @SerialName("items") val items: List<ReadReceiptItemDto>?,
    @SerialName("servicePercent") val servicePercent: Long?,
)
