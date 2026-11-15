package com.quare.blitzsplit.core.releasenotes.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Why: each version is read on its own, so one this app can't read doesn't take the others with it.
@Serializable
internal data class ReleaseNotesFeedDto(
    @SerialName("releases")
    val releases: List<JsonElement>,
)
