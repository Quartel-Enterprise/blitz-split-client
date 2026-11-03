package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: a term of the person's map and the category it files bills in, an app key or a listed category's id.
@Serializable
data class ReadReceiptTermDto(
    @SerialName("term") val term: String,
    @SerialName("categoryId") val categoryId: String,
)
