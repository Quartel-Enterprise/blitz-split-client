package com.quare.blitzsplit.core.sync.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountExportDto(
    // Why: "google", "apple", "anonymous" (a server ID, no account) or "none" (this device only).
    @SerialName("type") val type: String,
    @SerialName("userId") val userId: String?,
    @SerialName("email") val email: String?,
)
