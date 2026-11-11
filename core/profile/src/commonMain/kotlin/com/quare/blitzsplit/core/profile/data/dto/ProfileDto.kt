package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_color") val avatarColor: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("updated_at") val updatedAt: Long,
    // Why: the profile link's code; null from a server that doesn't have it yet, and sent only once there is one.
    @SerialName("share_code") val shareCode: String?,
)
