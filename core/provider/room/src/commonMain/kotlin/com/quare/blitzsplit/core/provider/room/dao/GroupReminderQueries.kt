package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity

/*
 * Why: the reminder's own writes, apart from the group row's Last-Write-Wins; GroupDao takes them on, so they share its
 * transactions.
 */
interface GroupReminderQueries {
    // Why: the stored schedule an edit compares with, to tell whether the reminder changed.
    @Query("SELECT * FROM split_group WHERE id = :id")
    suspend fun findGroup(id: String): GroupEntity?

    /*
     * Why: the server stamps the reminder's change apart from the row's Last-Write-Wins (an older app's change, or this
     * device's clamped to the server's clock). A row that wins brings its stamp with its schedule; one that loses still
     * raises the stamp, never lowers it.
     */
    @Query(
        "UPDATE split_group SET reminderUpdatedAt = :remoteAt " +
            "WHERE id = :id AND ownerUserId = :ownerUserId AND reminderUpdatedAt < :remoteAt",
    )
    suspend fun updateReminderUpdatedAtFromRemote(
        id: String,
        remoteAt: Long,
        ownerUserId: String,
    )

    // Why: "Dispensar" on the group's reminder banner: this device only, so the group isn't stamped nor pending.
    @Query(
        "UPDATE split_group SET reminderDismissedAt = :occurrenceAt WHERE id = :id AND reminderDismissedAt < :occurrenceAt",
    )
    suspend fun dismissReminder(
        id: String,
        occurrenceAt: Long,
    )
}
