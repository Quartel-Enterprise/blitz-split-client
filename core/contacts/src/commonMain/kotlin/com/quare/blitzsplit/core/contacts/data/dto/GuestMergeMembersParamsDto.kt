package com.quare.blitzsplit.core.contacts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuestMergeMembersParamsDto(
    @SerialName("p_member_ids") val memberIds: List<String>,
)
