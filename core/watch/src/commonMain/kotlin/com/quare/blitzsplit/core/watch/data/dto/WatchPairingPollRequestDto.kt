package com.quare.blitzsplit.core.watch.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchPairingPollRequestDto(
    @SerialName("secret") val secret: String,
)
