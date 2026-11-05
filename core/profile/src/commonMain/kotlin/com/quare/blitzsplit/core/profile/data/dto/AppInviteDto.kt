package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppInviteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("dismissed_at") val dismissedAt: Long,
)
