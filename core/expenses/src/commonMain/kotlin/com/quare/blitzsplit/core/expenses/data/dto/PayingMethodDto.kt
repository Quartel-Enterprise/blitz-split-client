package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayingMethodDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String,
    @SerialName("is_default") val isDefault: Boolean,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
