package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: what read-receipt read, amounts in the currency's minor unit, the date "YYYY-MM-DD" and the time "HH:MM".
 * What wasn't read may come as null or not at all, so a server that leaves a key out never fails the whole reading.
 */
@Serializable
data class ReadReceiptResponseDto(
    @SerialName("totalMinor") val totalMinor: Long,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("time") val time: String? = null,
    @SerialName("items") val items: List<ReadReceiptItemDto>? = null,
    @SerialName("servicePercent") val servicePercent: Long? = null,
)
