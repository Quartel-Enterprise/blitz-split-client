package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryTermDto(
    @SerialName("user_id") val userId: String,
    @SerialName("term_key") val termKey: String,
    @SerialName("term") val term: String,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
