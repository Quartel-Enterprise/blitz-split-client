package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one row, written only once a toggle changes, so a user who never touched one keeps the defaults. Synced
 * with the account, unlike the device settings. The first four are the push of Bills, Payments and settle-ups, the
 * open balance reminder and People and groups, named as when they were the only toggles, as the server's columns.
 */
@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @PrimaryKey
    val id: String,
    val newExpense: Boolean,
    val paymentMarked: Boolean,
    val settleReminders: Boolean,
    val invites: Boolean,
    // Why: the rows and channels added later default to on, as a row of an older version reads.
    @ColumnInfo(defaultValue = "1")
    val comments: Boolean,
    @ColumnInfo(defaultValue = "1")
    val paymentRequests: Boolean,
    @ColumnInfo(defaultValue = "1")
    val inAppBills: Boolean,
    @ColumnInfo(defaultValue = "1")
    val inAppPayments: Boolean,
    @ColumnInfo(defaultValue = "1")
    val inAppComments: Boolean,
    @ColumnInfo(defaultValue = "1")
    val inAppPeopleAndGroups: Boolean,
    @ColumnInfo(defaultValue = "1")
    val inAppPaymentRequests: Boolean,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose preferences these are, as ProfileEntity.ownerUserId, so a sync under another user drops them.
    val ownerUserId: String?,
)
