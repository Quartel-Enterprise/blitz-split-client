package com.quare.blitzsplit.core.releasenotes.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReleaseNotesFeedDto(
    @SerialName("releases")
    val releases: List<ReleaseDto>,
)
