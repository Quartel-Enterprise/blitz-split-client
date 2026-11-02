package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupCategoryDto(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    // Why: the server keeps whoever created it, whatever a later push sends.
    @SerialName("created_by") val createdBy: String?,
    @SerialName("created_by_member_id") val createdByMemberId: String?,
    @SerialName("parent_id") val parentId: String?,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String?,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
