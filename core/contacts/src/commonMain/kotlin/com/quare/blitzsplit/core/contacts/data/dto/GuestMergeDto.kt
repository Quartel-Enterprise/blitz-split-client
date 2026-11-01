package com.quare.blitzsplit.core.contacts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuestMergeDto(
    @SerialName("id") val id: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("member_ids") val memberIds: List<String>,
    @SerialName("group_id") val groupId: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("member_invite_code") val memberInviteCode: String,
    @SerialName("requested_at") val requestedAt: Long,
)
