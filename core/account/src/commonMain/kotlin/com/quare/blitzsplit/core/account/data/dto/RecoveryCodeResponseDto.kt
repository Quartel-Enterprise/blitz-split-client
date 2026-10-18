package com.quare.blitzsplit.core.account.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecoveryCodeResponseDto(
    @SerialName("code") val code: String,
)
