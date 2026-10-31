package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: one row per payment that settles a debt ("Registrar pagamento"), keyed by a UUID the device makes, so a
 * payment recorded offline keeps its id on the server. It goes with its group, like a bill.
 */
@Entity(
    tableName = "settlement",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class SettlementEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    // Why: the account that recorded it, null until the first sync of one recorded before any account.
    val createdBy: String?,
    // Why: the member who paid and the member who received.
    val fromMemberId: String,
    val toMemberId: String,
    // Why: in the minor unit of the group's currency.
    val amountMinor: Long,
    // Why: "Como foi pago", the server's code ("pix", "cash", "transfer", "other").
    val method: String,
    // Why: "Quando", which the group's list sorts by among the bills.
    val paidAt: Long,
    // Why: "Desfazer" keeps a tombstone, so every device drops the payment.
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
)
