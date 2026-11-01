package com.quare.blitzsplit.core.activity.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityReadDto(
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
)
