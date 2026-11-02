package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the two columns of a profile the faces need, which the server lets each member read of the others.
@Serializable
data class MemberPhotoDto(
    @SerialName("id") val userId: String,
    @SerialName("avatar_url") val photoUrl: String?,
)
