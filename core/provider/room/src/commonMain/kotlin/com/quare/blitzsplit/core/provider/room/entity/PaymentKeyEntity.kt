package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one row per key, keyed by a UUID the device makes, so a key added offline keeps its id on the server.
 * A deleted key stays as a tombstone until its deletion goes up.
 */
@Entity(tableName = "payment_key")
data class PaymentKeyEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val value: String,
    val isDefault: Boolean,
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose key this is, as ProfileEntity.ownerUserId, so a sync under another user drops it.
    val ownerUserId: String?,
)
