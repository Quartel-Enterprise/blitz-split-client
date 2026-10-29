package com.quare.blitzsplit.core.account.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: exactly one of the two is set; the function reads a null as missing.
@Serializable
data class AppleTokenRequestDto(
    @SerialName("authorizationCode") val authorizationCode: String?,
    @SerialName("refreshToken") val refreshToken: String?,
)
