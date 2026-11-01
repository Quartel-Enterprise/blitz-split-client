package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: the keys of the group's "Divisão padrão" this version knows, out of the object the server keeps in
 * groups.default_split and Room in its column.
 */
@Serializable
data class DefaultSplitDto(
    @SerialName("mode") val mode: String,
    @SerialName("weights") val weights: List<DefaultSplitWeightDto>,
)
