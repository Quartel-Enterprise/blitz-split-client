package com.quare.blitzsplit.core.sync.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the In app toggle of each row of Notifications that has one; the weekly reminder has none.
@Serializable
data class InAppNotificationsExportDto(
    @SerialName("bills") val bills: Boolean,
    @SerialName("payments") val payments: Boolean,
    @SerialName("comments") val comments: Boolean,
    @SerialName("peopleAndGroups") val peopleAndGroups: Boolean,
    @SerialName("paymentRequests") val paymentRequests: Boolean,
)
