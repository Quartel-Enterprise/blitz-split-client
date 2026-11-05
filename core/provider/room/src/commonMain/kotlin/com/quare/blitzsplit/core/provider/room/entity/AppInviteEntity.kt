package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: "Get the app" (page 14), one row: whether the invite was dismissed ("I downloaded it" or ✕), synced with the
 * account and kept here without one, and since when the account uses the Android and iPhone apps, as the server
 * last said, so Home and Profile know it offline too.
 */
@Entity(tableName = "app_invite")
data class AppInviteEntity(
    @PrimaryKey
    val id: String,
    // Why: this device's clock in epoch millis; a dismissal is final, so it never goes back to null but on a switch.
    val dismissedAt: Long?,
    // Why: a dismissal not on the server yet.
    val pendingSync: Boolean,
    // Why: the server's clock in epoch millis, the first sign in on each phone app; null while it never happened.
    val androidSeenAt: Long?,
    val iosSeenAt: Long?,
    // Why: as NotificationPreferencesEntity.ownerUserId, so a sync under another user drops another user's row.
    val ownerUserId: String?,
)
