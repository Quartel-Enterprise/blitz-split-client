package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the arguments of the leave_group and delete_group RPCs.
@Serializable
data class GroupIdParamsDto(
    @SerialName("p_group_id") val groupId: String,
)
