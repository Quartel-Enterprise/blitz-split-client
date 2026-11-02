package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: one of the default split's exceptions by category (6.7f), shaped like the default itself.
@Serializable
data class CategorySplitDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("mode") val mode: String,
    @SerialName("weights") val weights: List<DefaultSplitWeightDto>,
)
