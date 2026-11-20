package com.quare.blitzsplit.core.watch.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchPairingCodeDto(
    @SerialName("code") val code: String,
    @SerialName("secret") val secret: String,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("pollIntervalSeconds") val pollIntervalSeconds: Int,
)
