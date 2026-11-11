package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShareCodeParamsDto(
    @SerialName("p_code") val code: String,
)
