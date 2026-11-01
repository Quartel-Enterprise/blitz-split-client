package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// Why: an event the user opened, on this device (pendingSync until it goes up) or on another one of theirs.
@Entity(tableName = "activity_read")
data class ActivityReadEntity(
    @PrimaryKey
    val eventId: String,
    val pendingSync: Boolean,
)
