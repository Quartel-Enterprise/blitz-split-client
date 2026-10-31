package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: a group the server took off this user's list: one they left, or one a member deleted.
@Serializable
data class RemovedGroupDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    // Why: left or deleted.
    @SerialName("reason") val reason: String,
    // Why: null once that account is deleted.
    @SerialName("removed_by") val removedBy: String?,
    @SerialName("removed_by_name") val removedByName: String?,
)
