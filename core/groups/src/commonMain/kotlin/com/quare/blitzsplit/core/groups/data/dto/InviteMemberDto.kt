package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteMemberDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("avatar_color") val avatarColor: String,
    @SerialName("has_joined") val hasJoined: Boolean,
    // Why: null for no photo, and for a get_invite that doesn't answer photo_url yet, rather than failing the invite.
    @SerialName("photo_url") val photoUrl: String? = null,
)
