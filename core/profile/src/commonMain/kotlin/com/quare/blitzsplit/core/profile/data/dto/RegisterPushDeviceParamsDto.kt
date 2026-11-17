package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterPushDeviceParamsDto(
    @SerialName("p_token") val token: String,
    @SerialName("p_platform") val platform: String,
    @SerialName("p_apns_environment") val apnsEnvironment: String?,
    @SerialName("p_web_p256dh") val webP256dh: String?,
    @SerialName("p_web_auth") val webAuth: String?,
    // Why: the IANA name of this device's clock (the server fires each group's reminder at its time on it).
    @SerialName("p_time_zone") val timeZone: String? = null,
)
