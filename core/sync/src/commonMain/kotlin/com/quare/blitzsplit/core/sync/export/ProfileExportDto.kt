package com.quare.blitzsplit.core.sync.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileExportDto(
    @SerialName("displayName") val displayName: String,
    @SerialName("avatarColor") val avatarColor: String,
    // Why: a photo picked offline is still on the device, not at a URL, so it is left out until its upload.
    @SerialName("photoUrl") val photoUrl: String?,
)
