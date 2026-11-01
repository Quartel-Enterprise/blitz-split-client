package com.quare.blitzsplit.core.contacts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HiddenContactDto(
    @SerialName("user_id") val userId: String,
    @SerialName("person_key") val personKey: String,
    @SerialName("is_hidden") val isHidden: Boolean,
    @SerialName("hidden_at") val hiddenAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
