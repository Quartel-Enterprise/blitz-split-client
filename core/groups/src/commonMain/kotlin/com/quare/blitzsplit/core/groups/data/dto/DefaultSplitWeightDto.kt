package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: one member's weight in the group's "Divisão padrão".
@Serializable
data class DefaultSplitWeightDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("weight") val weight: Long,
)
