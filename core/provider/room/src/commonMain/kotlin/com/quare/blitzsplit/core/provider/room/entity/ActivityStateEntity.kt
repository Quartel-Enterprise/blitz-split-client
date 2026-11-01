package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: the one row of the feed's sync state on this device: whose feed it is, how far "Marcar tudo como lido" reads,
 * and whether the feed ever came down (a new device shows placeholders until then, a device with no account only what
 * it did itself).
 */
@Entity(tableName = "activity_state")
data class ActivityStateEntity(
    @PrimaryKey
    val id: String,
    // Why: the account whose feed this is; another one's is dropped on the next sync.
    val ownerUserId: String?,
    // Why: every event up to this seq is read.
    val readThroughSeq: Long,
    val markUpdatedAt: Long,
    val markPendingSync: Boolean,
    val hasDownloaded: Boolean,
)
