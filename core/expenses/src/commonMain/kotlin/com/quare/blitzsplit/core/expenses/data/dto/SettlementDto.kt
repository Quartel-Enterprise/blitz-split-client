package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettlementDto(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    // Why: the server keeps whoever recorded it, whatever a later push sends.
    @SerialName("created_by") val createdBy: String?,
    @SerialName("from_member_id") val fromMemberId: String,
    @SerialName("to_member_id") val toMemberId: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("method") val method: String,
    @SerialName("paid_at") val paidAt: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
