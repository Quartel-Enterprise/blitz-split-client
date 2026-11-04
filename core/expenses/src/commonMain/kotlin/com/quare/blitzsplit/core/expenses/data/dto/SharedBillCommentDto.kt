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
    // Why: null on a server older than the comments' edits and likes.
    @SerialName("edited_at") val editedAt: Long? = null,
    @SerialName("is_deleted") val isDeleted: Boolean? = null,
    @SerialName("is_mine") val isMine: Boolean? = null,
    @SerialName("liked_by") val likedBy: List<SharedBillLikeDto>? = null,
)

@Serializable
data class SharedBillLikeDto(
    @SerialName("name") val name: String?,
    @SerialName("is_mine") val isMine: Boolean,
)
