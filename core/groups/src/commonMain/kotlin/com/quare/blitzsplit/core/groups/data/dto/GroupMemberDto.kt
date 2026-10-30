package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberDto(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String?,
    @SerialName("name") val name: String,
    @SerialName("avatar_color") val avatarColor: String,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    // Why: the server makes it and keeps it whatever a push sends, which is always null.
    @SerialName("invite_code") val inviteCode: String?,
)
