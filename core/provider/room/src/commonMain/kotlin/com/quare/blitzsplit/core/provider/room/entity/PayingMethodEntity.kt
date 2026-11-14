package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one of the person's payment methods ("Payment methods" in Profile), keyed by a UUID the device makes, so one
 * added offline keeps its id on the server. A deleted one stays as a tombstone until its deletion goes up.
 */
@Entity(tableName = "paying_method")
data class PayingMethodEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    // Why: a card's color code, empty for none.
    val icon: String,
    val isDefault: Boolean,
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose method this is, as PaymentKeyEntity.ownerUserId, so a sync under another user drops it.
    val ownerUserId: String?,
)
