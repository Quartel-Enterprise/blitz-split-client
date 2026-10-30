package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the argument of the get_invite RPC.
@Serializable
data class InviteCodeParamsDto(
    @SerialName("p_code") val code: String,
)
