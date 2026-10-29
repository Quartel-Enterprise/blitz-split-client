package com.quare.blitzsplit.core.sync.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferencesExportDto(
    @SerialName("newExpense") val newExpense: Boolean,
    @SerialName("paymentMarked") val paymentMarked: Boolean,
    @SerialName("settleReminders") val settleReminders: Boolean,
    @SerialName("invites") val invites: Boolean,
)
