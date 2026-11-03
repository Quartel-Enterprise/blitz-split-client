package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: read-receipt's body: the photo as a base64 JPEG and the group's currency, which the amounts come back in.
@Serializable
data class ReadReceiptRequestDto(
    @SerialName("image") val image: String,
    @SerialName("currency") val currency: String,
)
