package com.quare.blitzsplit.core.sync.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentKeyExportDto(
    @SerialName("method") val method: String,
    @SerialName("type") val type: String,
    @SerialName("value") val value: String,
    @SerialName("isDefault") val isDefault: Boolean,
    @SerialName("addedAt") val addedAt: String,
)
