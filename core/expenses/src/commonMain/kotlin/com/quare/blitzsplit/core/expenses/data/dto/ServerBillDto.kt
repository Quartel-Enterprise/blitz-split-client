package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerBillDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
