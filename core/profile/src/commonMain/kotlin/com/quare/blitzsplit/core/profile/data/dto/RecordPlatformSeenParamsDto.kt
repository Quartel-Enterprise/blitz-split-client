package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordPlatformSeenParamsDto(
    @SerialName("p_platform") val platform: String,
)
