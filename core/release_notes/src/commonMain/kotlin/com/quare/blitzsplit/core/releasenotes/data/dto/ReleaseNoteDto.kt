package com.quare.blitzsplit.core.releasenotes.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReleaseNoteDto(
    @SerialName("text")
    val text: String,
    @SerialName("platforms")
    val platforms: List<String>,
)
