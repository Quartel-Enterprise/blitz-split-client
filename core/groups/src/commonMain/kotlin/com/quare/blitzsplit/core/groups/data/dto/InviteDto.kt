package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("color") val color: String,
    @SerialName("currency") val currency: String,
    @SerialName("creator_name") val creatorName: String?,
    @SerialName("is_member") val isMember: Boolean,
    @SerialName("members") val members: List<InviteMemberDto>,
    @SerialName("personal_member_id") val personalMemberId: String? = null,
    @SerialName("inviter_name") val inviterName: String? = null,
)
