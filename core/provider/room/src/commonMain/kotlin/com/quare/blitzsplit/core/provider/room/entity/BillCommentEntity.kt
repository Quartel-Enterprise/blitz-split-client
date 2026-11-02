package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: a comment on a bill ("Comentários", 7.3), keyed by a UUID the device makes, so one written offline keeps its id
 * on the server. Never edited; it goes with its bill.
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
    val pendingSync: Boolean,
)
