package com.quare.blitzsplit.core.watch.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchPairingApproveRequestDto(
    @SerialName("code") val code: String,
)
