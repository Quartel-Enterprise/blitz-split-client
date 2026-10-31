package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: what leave_group and delete_group answer: left or deleted, has_balance, or not_member.
@Serializable
data class GroupExitResultDto(
    @SerialName("status") val status: String,
)
