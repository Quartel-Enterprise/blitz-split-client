package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: one row per bill, keyed by a UUID the device makes, so a bill created offline keeps its id on the server. It
 * goes with its group: a sync that drops the group (a switch, a group gone from the server, a sign out) drops its
 * bills too.
 */
@Entity(
    tableName = "expense",
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
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    // Why: the account that created the bill, null until the first sync of a bill created before any account.
    val createdBy: String?,
    val description: String,
    // Why: in the minor unit of the group's currency, so no amount is ever a fraction.
    val amountMinor: Long,
    val splitMode: String,
    /*
     * Why: who paid and each member's part, as the server's jsonb, so the whole bill is the unit of Last-Write-Wins
     * and nothing reads one without the others.
     */
    val payersJson: String,
    val sharesJson: String,
    // Why: "Itens da conta" the shares come from, as the server's jsonb; null for a bill split any other way.
    val itemsJson: String?,
    // Why: "Quando", which the group's list sorts and groups by day.
    val spentAt: Long,
    // Why: "Apagar conta" keeps a tombstone, so every device drops the bill.
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    /*
     * Why: the account that last saved the bill, which the server sets; "Ana mudou esta conta" while someone edits it.
     * Null for a bill changed on this device and not synced yet.
     */
    val updatedBy: String?,
    val pendingSync: Boolean,
)
