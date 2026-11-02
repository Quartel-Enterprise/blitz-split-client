package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: what get_shared_bill answers: only status for "not_found" and "members_only", everything for "ok".
@Serializable
data class SharedBillDto(
    @SerialName("status") val status: String,
    @SerialName("is_member") val isMember: Boolean?,
    @SerialName("can_comment") val canComment: Boolean?,
    @SerialName("group_id") val groupId: String?,
    @SerialName("group_name") val groupName: String?,
    @SerialName("currency") val currency: String?,
    @SerialName("visibility") val visibility: String?,
    @SerialName("bill") val bill: SharedBillExpenseDto?,
    @SerialName("attachments") val attachments: List<SharedBillAttachmentDto>?,
    @SerialName("comments") val comments: List<SharedBillCommentDto>?,
)
