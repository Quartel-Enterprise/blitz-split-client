package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JoinGroupResultDto(
    @SerialName("status") val status: String,
    // Why: null when the code matches no group.
    @SerialName("group_id") val groupId: String?,
)
