package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentOnSharedBillParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_id") val id: String,
    @SerialName("p_body") val body: String,
    @SerialName("p_thread_id") val threadId: String?,
    @SerialName("p_reply_to_name") val replyToName: String?,
    @SerialName("p_author_name") val authorName: String?,
)
