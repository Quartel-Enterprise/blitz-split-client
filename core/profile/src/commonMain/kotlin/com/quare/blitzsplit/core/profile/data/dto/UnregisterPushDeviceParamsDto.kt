package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnregisterPushDeviceParamsDto(
    @SerialName("p_token") val token: String,
)
