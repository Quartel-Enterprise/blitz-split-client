package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the argument of the rotate_invite_code RPC.
@Serializable
data class RotateInviteCodeParamsDto(
    @SerialName("p_group_id") val groupId: String,
)
