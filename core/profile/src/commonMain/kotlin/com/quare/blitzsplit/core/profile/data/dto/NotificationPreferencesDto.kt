package com.quare.blitzsplit.core.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: the first four are the push of Bills, Payments and settle-ups, the open balance reminder and People and
 * groups, named as when they were the only toggles. The server fills the later ones with its defaults (on) for a row an
 * older version wrote, so every row reads whole.
 */
@Serializable
data class NotificationPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("new_expense") val newExpense: Boolean,
    @SerialName("payment_marked") val paymentMarked: Boolean,
    @SerialName("settle_reminders") val settleReminders: Boolean,
    @SerialName("invites") val invites: Boolean,
    @SerialName("comments") val comments: Boolean,
    @SerialName("payment_requests") val paymentRequests: Boolean,
    @SerialName("in_app_bills") val inAppBills: Boolean,
    @SerialName("in_app_payments") val inAppPayments: Boolean,
    @SerialName("in_app_comments") val inAppComments: Boolean,
    @SerialName("in_app_people_and_groups") val inAppPeopleAndGroups: Boolean,
    @SerialName("in_app_payment_requests") val inAppPaymentRequests: Boolean,
    @SerialName("updated_at") val updatedAt: Long,
)
