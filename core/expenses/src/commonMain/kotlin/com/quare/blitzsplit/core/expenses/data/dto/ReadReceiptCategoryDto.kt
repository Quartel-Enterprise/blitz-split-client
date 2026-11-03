package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: one of the group's own categories, which read-receipt may answer besides the app's.
@Serializable
data class ReadReceiptCategoryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    // Why: an app main category's key or another listed category's id; null for a main one.
    @SerialName("parentId") val parentId: String?,
)
