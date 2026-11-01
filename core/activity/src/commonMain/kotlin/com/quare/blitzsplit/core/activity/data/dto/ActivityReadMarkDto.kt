package com.quare.blitzsplit.core.activity.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityReadMarkDto(
    @SerialName("user_id") val userId: String,
    @SerialName("read_through_seq") val readThroughSeq: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
