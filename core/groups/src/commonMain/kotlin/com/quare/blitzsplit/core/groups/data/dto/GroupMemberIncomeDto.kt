package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberIncomeDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("income_minor") val incomeMinor: Long,
    // Why: the server checks it is whoever pushes, as group_categories' created_by.
    @SerialName("updated_by") val updatedBy: String?,
    @SerialName("updated_at") val updatedAt: Long,
)
