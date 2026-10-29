package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one row, written only once a toggle changes, so a user who never touched one keeps the defaults. Synced
 * with the account, unlike the device settings.
 */
@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @PrimaryKey
    val id: String,
    val newExpense: Boolean,
    val paymentMarked: Boolean,
    val settleReminders: Boolean,
    val invites: Boolean,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose preferences these are, as ProfileEntity.ownerUserId, so a sync under another user drops them.
    val ownerUserId: String?,
)
