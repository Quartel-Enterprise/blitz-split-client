package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: the live keys of the people this device's user shares a group with, as the server last gave them, so
 * "Pagar Ana" (4.5) shows them offline too. Read only: each sync replaces them all, which also drops a key deleted
 * or a person no longer in any group with the user.
 */
@Entity(
    tableName = "member_payment_key",
    indices = [Index("userId")],
)
data class MemberPaymentKeyEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String,
    val value: String,
    val isDefault: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
)
