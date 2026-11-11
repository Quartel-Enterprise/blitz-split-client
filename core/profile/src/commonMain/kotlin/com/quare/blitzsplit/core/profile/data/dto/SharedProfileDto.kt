package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: a row of get_profile_by_share_code; user_id only for the user and whoever already shares a group with them.
@Serializable
data class SharedProfileDto(
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_color") val avatarColor: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("user_id") val userId: String?,
)
