package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedBillAttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: String,
    @SerialName("path") val path: String,
    @SerialName("width") val width: Int?,
    @SerialName("height") val height: Int?,
    @SerialName("added_by_name") val addedByName: String?,
    @SerialName("added_at") val addedAt: Long,
)
