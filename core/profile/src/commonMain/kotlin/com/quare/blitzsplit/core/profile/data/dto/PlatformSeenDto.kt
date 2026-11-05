package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlatformSeenDto(
    @SerialName("platform") val platform: String,
    @SerialName("first_seen_at") val firstSeenAt: Long,
)
