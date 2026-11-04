package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: a comment on a bill ("Comentários", 7.3), keyed by a UUID the device makes, so one written offline keeps its id
 * on the server. Only its author edits or deletes it; it goes with its bill.
 */
@Entity(
    tableName = "bill_comment",
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
data class BillCommentEntity(
    @PrimaryKey
    val id: String,
    val expenseId: String,
    val groupId: String,
    // Why: null until the server stamps the account of one written before it went up.
    val authorUserId: String?,
    // Why: null for someone who commented through the bill's open link, out of the group.
    val authorMemberId: String?,
    val authorName: String?,
    // Why: the top comment of its thread, null for a top comment.
    val threadId: String?,
    // Why: whom a reply to a reply answers ("Respondendo a Diego").
    val replyToName: String?,
    val body: String,
    val viaLink: Boolean,
    val postedAt: Long,
    // Why: when its author last edited it ("editado"), the server's Last-Write-Wins timestamp; null while never edited.
    @ColumnInfo(defaultValue = "NULL")
    val editedAt: Long?,
    // Why: deleted by its author, a tombstone with no text that keeps its thread's replies under "Comentário apagado".
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean,
    // Why: written, edited or deleted here and not up yet.
    val pendingSync: Boolean,
)
