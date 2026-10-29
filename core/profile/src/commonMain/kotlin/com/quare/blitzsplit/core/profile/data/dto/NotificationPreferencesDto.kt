package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("new_expense") val newExpense: Boolean,
    @SerialName("payment_marked") val paymentMarked: Boolean,
    @SerialName("settle_reminders") val settleReminders: Boolean,
    @SerialName("invites") val invites: Boolean,
    @SerialName("updated_at") val updatedAt: Long,
)
