package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedBillCommentDto(
    @SerialName("id") val id: String,
    @SerialName("thread_id") val threadId: String?,
    @SerialName("author_name") val authorName: String?,
    @SerialName("reply_to_name") val replyToName: String?,
    @SerialName("body") val body: String,
    @SerialName("via_link") val viaLink: Boolean?,
    @SerialName("posted_at") val postedAt: Long,
    // Why: the comments' edits and likes (beta feedback 14).
    @SerialName("edited_at") val editedAt: Long?,
    @SerialName("is_deleted") val isDeleted: Boolean?,
    @SerialName("is_mine") val isMine: Boolean?,
    @SerialName("liked_by") val likedBy: List<SharedBillLikeDto>?,
)
