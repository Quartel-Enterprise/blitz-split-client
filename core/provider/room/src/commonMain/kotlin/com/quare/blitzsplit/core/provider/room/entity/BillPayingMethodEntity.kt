package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: which of their payment methods this device's user paid a bill with ("Paid with"), one row per bill. Only they
 * see it; a null methodId is a mark cleared, kept so the clearing reaches their other devices.
 */
@Entity(tableName = "bill_paying_method")
data class BillPayingMethodEntity(
    @PrimaryKey
    val expenseId: String,
    val methodId: String?,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: as PayingMethodEntity.ownerUserId.
    val ownerUserId: String?,
)
