package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LikeSharedBillCommentParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_id") val id: String,
    @SerialName("p_liked") val liked: Boolean,
)
