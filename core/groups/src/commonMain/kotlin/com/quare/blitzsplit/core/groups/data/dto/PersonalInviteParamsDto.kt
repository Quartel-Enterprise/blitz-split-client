package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the arguments of the get_invite RPC for a personal link.
@Serializable
data class PersonalInviteParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_member_code") val memberCode: String,
    @SerialName("p_inviter_code") val inviterCode: String?,
)
