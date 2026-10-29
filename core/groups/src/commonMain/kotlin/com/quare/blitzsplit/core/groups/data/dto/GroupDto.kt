package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDto(
    @SerialName("id") val id: String,
    @SerialName("created_by") val createdBy: String?,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("color") val color: String,
    @SerialName("currency") val currency: String,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
