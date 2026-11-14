package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one color of the person's palette, created with "+" in a category's color picker and synced with the account
 * (the server's custom_colors). Keyed by the color itself, "#3A7BD5", so the same color added on two devices is one row.
 * A deleted one stays (isDeleted), so its deletion still wins over an older add from another device.
 */
@Entity(tableName = "custom_color")
data class CustomColorEntity(
    @PrimaryKey
    val color: String,
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose palette this is, as CategoryTermEntity.ownerUserId, so a sync under another user drops it.
    val ownerUserId: String?,
)
