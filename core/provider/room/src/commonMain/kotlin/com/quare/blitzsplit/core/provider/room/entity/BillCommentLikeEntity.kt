package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/*
 * Why: a like on a bill's comment, one per person: this device's user's own under ME, which their account's row on the
 * server comes down as, and everyone else's under their account. Undoing one keeps the row unliked, so the newer
 * toggle wins between devices; it goes with its comment.
 */
@Entity(
    tableName = "bill_comment_like",
    primaryKeys = ["commentId", "likerKey"],
    foreignKeys = [
        ForeignKey(
            entity = BillCommentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class BillCommentLikeEntity(
    val commentId: String,
    // Why: ME for this device's user, else the liker's account.
    val likerKey: String,
    val groupId: String,
    // Why: the liker's member in the group, null through the bill's link.
    val memberId: String?,
    // Why: the name the server lists it under, for someone out of the group.
    val likerName: String?,
    val isLiked: Boolean,
    val updatedAt: Long,
    val pendingSync: Boolean,
) {
    companion object {
        const val ME = "me"
    }
}
