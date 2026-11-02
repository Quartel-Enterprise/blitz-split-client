package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillAttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("expense_id") val expenseId: String,
    // Why: the server takes the group and who added it from the bill and the caller.
    @SerialName("group_id") val groupId: String,
    @SerialName("kind") val kind: String,
    @SerialName("added_by") val addedByUserId: String?,
    @SerialName("added_by_member_id") val addedByMemberId: String?,
    @SerialName("width") val width: Int?,
    @SerialName("height") val height: Int?,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
