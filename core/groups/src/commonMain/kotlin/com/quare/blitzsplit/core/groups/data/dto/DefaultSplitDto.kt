package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the group's "Divisão padrão" as the server keeps it in groups.default_split, and Room in its column.
@Serializable
data class DefaultSplitDto(
    @SerialName("mode") val mode: String,
    @SerialName("weights") val weights: List<DefaultSplitWeightDto>,
)
