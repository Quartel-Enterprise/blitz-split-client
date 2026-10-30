package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the arguments of the join_group RPC, with either a member or a new name.
@Serializable
data class JoinGroupParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_avatar_color") val avatarColor: String,
    @SerialName("p_member_id") val memberId: String?,
    @SerialName("p_new_name") val newName: String?,
)
