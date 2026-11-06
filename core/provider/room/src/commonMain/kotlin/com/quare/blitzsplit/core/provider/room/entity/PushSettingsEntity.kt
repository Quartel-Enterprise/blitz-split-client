package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: Notifications' "Push notifications" switch, one row: whether this device takes pushes, apart from the system's
 * permission. Per device like the permission, so it is never synced and outlives a sign out; no row yet is on.
 */
@Entity(tableName = "push_settings")
data class PushSettingsEntity(
    @PrimaryKey
    val id: String,
    val isEnabled: Boolean,
)
