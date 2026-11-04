package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedBillLikeDto(
    @SerialName("name") val name: String?,
    @SerialName("is_mine") val isMine: Boolean,
)
