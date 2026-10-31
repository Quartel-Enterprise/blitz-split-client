package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the argument of the release_member RPC.
@Serializable
data class ReleaseMemberParamsDto(
    @SerialName("p_member_id") val memberId: String,
)
