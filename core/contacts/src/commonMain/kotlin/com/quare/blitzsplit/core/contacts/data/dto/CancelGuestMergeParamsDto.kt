package com.quare.blitzsplit.core.contacts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CancelGuestMergeParamsDto(
    @SerialName("p_merge_id") val mergeId: String,
)
