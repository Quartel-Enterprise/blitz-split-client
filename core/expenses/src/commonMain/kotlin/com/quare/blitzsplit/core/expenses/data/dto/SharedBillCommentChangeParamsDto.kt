package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditSharedBillCommentParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_id") val id: String,
    @SerialName("p_body") val body: String,
)

@Serializable
data class DeleteSharedBillCommentParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_id") val id: String,
)

@Serializable
data class LikeSharedBillCommentParamsDto(
    @SerialName("p_code") val code: String,
    @SerialName("p_id") val id: String,
    @SerialName("p_liked") val liked: Boolean,
)
