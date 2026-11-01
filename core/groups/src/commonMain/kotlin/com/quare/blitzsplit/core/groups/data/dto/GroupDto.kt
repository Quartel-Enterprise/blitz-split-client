package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GroupDto(
    @SerialName("id") val id: String,
    @SerialName("created_by") val createdBy: String?,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("color") val color: String,
    @SerialName("currency") val currency: String,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    // Why: the server makes it and keeps it whatever a push sends, which is always null.
    @SerialName("invite_code") val inviteCode: String?,
    /*
     * Why: null while the group splits equally among everyone, sent as null so going back to it reaches the server.
     * Kept as the whole object, so the keys a newer app adds to it (DefaultSplitDto reads the ones this version
     * knows) go back up untouched with any change this device pushes.
     */
    @SerialName("default_split") val defaultSplit: JsonObject?,
)
