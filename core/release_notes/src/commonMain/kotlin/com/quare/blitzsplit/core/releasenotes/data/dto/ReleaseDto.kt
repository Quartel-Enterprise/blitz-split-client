package com.quare.blitzsplit.core.releasenotes.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReleaseDto(
    @SerialName("version")
    val version: String,
    @SerialName("date")
    val date: String,
    @SerialName("notes")
    val notes: List<ReleaseNoteDto>,
)
