package com.quare.blitzsplit.core.watch.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchPairingPollResponseDto(
    @SerialName("status") val status: String,
    @SerialName("tokenHash") val tokenHash: String?,
)
