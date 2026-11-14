package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the columns of a profile the faces and the profile link need, which the server lets each member read of the others.
@Serializable
data class MemberPhotoDto(
    @SerialName("id") val userId: String,
    @SerialName("avatar_url") val photoUrl: String?,
    @SerialName("share_code") val shareCode: String? = null,
)
