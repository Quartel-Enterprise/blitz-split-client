package com.quare.blitzsplit.core.inappupdate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AppStoreLookupResultDto(
    @SerialName("version") val version: String?,
    @SerialName("trackViewUrl") val trackViewUrl: String?,
)
