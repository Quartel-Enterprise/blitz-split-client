package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: a photo or the receipt of a bill ("Anexos", 7.3g), keyed by a UUID the device makes, which also names its
 * image in the `bill-attachments` bucket. Added offline, it keeps its JPEG here until the image goes up; taken off,
 * it stays as a tombstone until that goes up too. Last-Write-Wins on `updatedAt`; it goes with its bill.
 */
@Suppress("ArrayInDataClass")
@Entity(
    tableName = "bill_attachment",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("expenseId")],
)
data class BillAttachmentEntity(
    @PrimaryKey
    val id: String,
    val expenseId: String,
    val groupId: String,
    // Why: "photo", one of the 5, or "receipt".
    val kind: String,
    val width: Int?,
    val height: Int?,
    // Why: null until the server stamps the account of one added before it went up.
    val addedByUserId: String?,
    val addedByMemberId: String?,
    val addedAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    // Why: the image added here, until it is in the bucket.
    val pendingJpeg: ByteArray?,
    val pendingSync: Boolean,
)
