package com.quare.blitzsplit.core.activity.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ActivityEventDto(
    @SerialName("id") val id: String,
    @SerialName("seq") val seq: Long,
    @SerialName("group_id") val groupId: String,
    @SerialName("kind") val kind: String,
    @SerialName("actor_user_id") val actorUserId: String?,
    @SerialName("actor_member_id") val actorMemberId: String?,
    @SerialName("target_id") val targetId: String?,
    @SerialName("occurred_at") val occurredAt: Long,
    @SerialName("data") val data: JsonObject,
)
