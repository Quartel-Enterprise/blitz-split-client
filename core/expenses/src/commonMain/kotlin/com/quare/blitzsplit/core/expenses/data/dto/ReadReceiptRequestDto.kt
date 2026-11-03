package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: read-receipt's body: the photo as a base64 JPEG, the group's currency, which the amounts come back in, and
 * the group's categories and the person's terms the reading may choose the category by.
 */
@Serializable
data class ReadReceiptRequestDto(
    @SerialName("image") val image: String,
    @SerialName("currency") val currency: String,
    @SerialName("categories") val categories: List<ReadReceiptCategoryDto>,
    @SerialName("terms") val terms: List<ReadReceiptTermDto>,
)
