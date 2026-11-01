package com.quare.blitzsplit.core.activity.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Why: the one kind a client records ("charge_sent"); the server stamps the rest and assigns seq.
@Serializable
data class ChargeEventDto(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("kind") val kind: String,
    @SerialName("actor_user_id") val actorUserId: String,
    @SerialName("target_id") val targetId: String?,
    @SerialName("occurred_at") val occurredAt: Long,
    @SerialName("data") val data: JsonObject,
)
