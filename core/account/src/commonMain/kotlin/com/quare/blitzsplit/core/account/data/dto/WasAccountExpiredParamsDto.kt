package com.quare.blitzsplit.core.account.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WasAccountExpiredParamsDto(
    @SerialName("p_user_id")
    val userId: String,
)
