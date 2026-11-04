package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: a like as the server keeps it, undone ones included.
@Serializable
data class RemoteBillCommentLikeDto(
    @SerialName("id") val id: String,
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("member_id") val memberId: String?,
    @SerialName("liker_name") val likerName: String?,
    @SerialName("is_liked") val isLiked: Boolean,
    @SerialName("updated_at") val updatedAt: Long,
)
