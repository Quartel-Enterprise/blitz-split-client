package com.quare.blitzsplit.core.sync.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface RemoteChangeDto {
    data object Joined : RemoteChangeDto

    // Why: the columns of an activity_events insert the pull needs; the payload itself is never applied.
    @Serializable
    data class ActivityInsert(
        @SerialName("seq") val seq: Long,
        @SerialName("group_id") val groupId: String,
        @SerialName("kind") val kind: String,
    ) : RemoteChangeDto

    // Why: only who; the profile and the photos come down through the tables.
    @Serializable
    data class ProfileUpdate(
        @SerialName("id") val id: String,
    ) : RemoteChangeDto
}
