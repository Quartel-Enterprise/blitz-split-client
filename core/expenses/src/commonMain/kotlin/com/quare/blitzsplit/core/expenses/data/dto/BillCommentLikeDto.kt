package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: what the device sends of its own likes; the server stamps the group, the member and the name.
@Serializable
data class BillCommentLikeDto(
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("is_liked") val isLiked: Boolean,
    @SerialName("updated_at") val updatedAt: Long,
)

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
