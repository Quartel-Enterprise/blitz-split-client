package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: a row of the first_download_groups RPC, the groups a first download brings, the latest activity first.
@Serializable
data class FirstDownloadGroupDto(
    @SerialName("group_id") val groupId: String,
)
