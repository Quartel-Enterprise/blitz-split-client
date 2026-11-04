package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillCommentDto(
    @SerialName("id") val id: String,
    @SerialName("expense_id") val expenseId: String,
    // Why: the server takes the group, the author, the thread's top and the moment from the bill and the caller.
    @SerialName("group_id") val groupId: String,
    @SerialName("author_user_id") val authorUserId: String?,
    @SerialName("author_member_id") val authorMemberId: String?,
    @SerialName("author_name") val authorName: String?,
    @SerialName("thread_id") val threadId: String?,
    @SerialName("reply_to_name") val replyToName: String?,
    @SerialName("body") val body: String,
    @SerialName("via_link") val viaLink: Boolean,
    @SerialName("posted_at") val postedAt: Long,
    // Why: the server keeps the newer edit, and the tombstone over any edit.
    @SerialName("edited_at") val editedAt: Long?,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
