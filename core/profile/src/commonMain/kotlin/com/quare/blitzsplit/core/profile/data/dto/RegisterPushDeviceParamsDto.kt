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
)
