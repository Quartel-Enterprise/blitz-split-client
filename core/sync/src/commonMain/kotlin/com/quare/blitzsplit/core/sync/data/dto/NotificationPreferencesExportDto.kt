package com.quare.blitzsplit.core.sync.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: the push of each row, the first four named as when they were the only toggles (Bills, Payments and
 * settle-ups, the open balance reminder, People and groups), and the in-app channel of each row that has one.
 */
@Serializable
data class NotificationPreferencesExportDto(
    @SerialName("newExpense") val newExpense: Boolean,
    @SerialName("paymentMarked") val paymentMarked: Boolean,
    @SerialName("settleReminders") val settleReminders: Boolean,
    @SerialName("invites") val invites: Boolean,
    @SerialName("comments") val comments: Boolean,
    @SerialName("paymentRequests") val paymentRequests: Boolean,
    @SerialName("inApp") val inApp: InAppNotificationsExportDto,
)
