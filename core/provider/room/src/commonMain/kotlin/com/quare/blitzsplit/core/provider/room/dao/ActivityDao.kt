package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.ActivityBackfillEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityReadEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityStateEntity
import com.quare.blitzsplit.core.provider.room.entity.QueuedBillRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedGroupRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedPaymentRow
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_IDS_PER_QUERY = 500

@Dao
interface ActivityDao {
    // Why: the most recent first; the window is 90 days, so the feed filters and pages in memory.
    @Query("SELECT * FROM activity_event ORDER BY occurredAt DESC, seq DESC")
    fun observeEvents(): Flow<List<ActivityEventEntity>>

    @Query("SELECT eventId FROM activity_read")
    fun observeReadEventIds(): Flow<List<String>>

    @Query("SELECT * FROM activity_state WHERE id = '$SINGLE_ROW_ID'")
    fun observeState(): Flow<ActivityStateEntity?>

    @Query("SELECT * FROM activity_state WHERE id = '$SINGLE_ROW_ID'")
    suspend fun getState(): ActivityStateEntity?

    @Query(
        "SELECT e.id, e.groupId, e.description, e.amountMinor, e.splitMode, e.isDeleted, e.updatedAt, " +
            "EXISTS(SELECT 1 FROM activity_event a WHERE a.targetId = e.id AND a.seq IS NOT NULL) AS hasEvent " +
            "FROM expense e WHERE e.pendingSync = 1",
    )
    fun observeQueuedBills(): Flow<List<QueuedBillRow>>

    @Query(
        "SELECT id, groupId, fromMemberId, toMemberId, amountMinor, method, isDeleted, updatedAt " +
            "FROM settlement WHERE pendingSync = 1",
    )
    fun observeQueuedPayments(): Flow<List<QueuedPaymentRow>>

    @Query("SELECT id, name, type, inviteCode IS NULL AS isNew, updatedAt FROM split_group WHERE pendingSync = 1")
    fun observeQueuedGroups(): Flow<List<QueuedGroupRow>>

    // Why: a bill no longer here was deleted, so its events keep their row with the trash icon and open nothing.
    @Query("SELECT id FROM expense WHERE isDeleted = 0")
    fun observeLiveBillIds(): Flow<List<String>>

    @Query("SELECT id FROM split_group")
    fun observeLiveGroupIds(): Flow<List<String>>

    @Query("SELECT MAX(seq) FROM activity_event")
    suspend fun getMaxSeq(): Long?

    @Query("SELECT MIN(seq) FROM activity_event")
    suspend fun getMinSeq(): Long?

    @Query("SELECT * FROM activity_event WHERE pendingSync = 1")
    suspend fun getPendingEvents(): List<ActivityEventEntity>

    @Query("SELECT eventId FROM activity_read WHERE pendingSync = 1")
    suspend fun getPendingReadIds(): List<String>

    @Query("SELECT id FROM split_group WHERE id NOT IN (SELECT groupId FROM activity_backfill)")
    suspend fun getGroupIdsToBackfill(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBackfilled(entities: List<ActivityBackfillEntity>)

    // Why: what the server answers is its version, which a charge sent here takes once it went up.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(entities: List<ActivityEventEntity>)

    @Insert
    suspend fun insertEvents(entities: List<ActivityEventEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReads(entities: List<ActivityReadEntity>)

    @Query("UPDATE activity_event SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markEventsSynced(ids: List<String>)

    @Query("UPDATE activity_read SET pendingSync = 0 WHERE eventId IN (:ids)")
    suspend fun markReadsSynced(ids: List<String>)

    @Query(
        "INSERT OR IGNORE INTO activity_state (id, ownerUserId, readThroughSeq, markUpdatedAt, markPendingSync, " +
            "hasDownloaded) VALUES ('$SINGLE_ROW_ID', NULL, 0, 0, 0, 0)",
    )
    suspend fun ensureState()

    // Why: "Marcar tudo como lido" only moves forward, and the reads it now covers are no longer needed.
    @Transaction
    suspend fun markAllRead(
        throughSeq: Long,
        now: Long,
    ) {
        ensureState()
        updateMark(throughSeq, now)
        deleteReadsThrough(throughSeq)
    }

    @Query(
        "UPDATE activity_state SET readThroughSeq = MAX(readThroughSeq, :throughSeq), " +
            "markUpdatedAt = MAX(:now, markUpdatedAt + 1), markPendingSync = 1 WHERE id = '$SINGLE_ROW_ID'",
    )
    suspend fun updateMark(
        throughSeq: Long,
        now: Long,
    )

    @Query(
        "DELETE FROM activity_read WHERE pendingSync = 0 AND eventId IN " +
            "(SELECT id FROM activity_event WHERE seq <= :throughSeq)",
    )
    suspend fun deleteReadsThrough(throughSeq: Long)

    // Why: the server's mark only moves forward too, so the further of the two wins, still pending if this one is.
    @Transaction
    suspend fun applyRemoteMark(throughSeq: Long) {
        ensureState()
        applyMark(throughSeq)
    }

    @Query(
        "UPDATE activity_state SET readThroughSeq = MAX(readThroughSeq, :throughSeq), " +
            "markPendingSync = markPendingSync AND readThroughSeq > :throughSeq WHERE id = '$SINGLE_ROW_ID'",
    )
    suspend fun applyMark(throughSeq: Long)

    // Why: a mark moved again while the push was in flight stays pending.
    @Query(
        "UPDATE activity_state SET markPendingSync = 0 WHERE id = '$SINGLE_ROW_ID' AND markUpdatedAt = :syncedUpdatedAt",
    )
    suspend fun markMarkSynced(syncedUpdatedAt: Long)

    @Query("UPDATE activity_state SET ownerUserId = :userId, hasDownloaded = 1 WHERE id = '$SINGLE_ROW_ID'")
    suspend fun markDownloaded(userId: String)

    /*
     * Why: the device keeps 90 days to read offline, and the events of the groups it no longer has but the one saying a
     * group was deleted. Pending ones stay, waiting to go up.
     */
    @Transaction
    suspend fun prune(cutoff: Long) {
        deleteOlderThan(cutoff)
        deleteOfGoneGroups()
    }

    @Query("DELETE FROM activity_event WHERE pendingSync = 0 AND occurredAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query(
        "DELETE FROM activity_event WHERE pendingSync = 0 AND kind <> 'group_deleted' " +
            "AND groupId NOT IN (SELECT id FROM split_group)",
    )
    suspend fun deleteOfGoneGroups()

    // Why: in chunks, below SQLite's cap on bound parameters.
    @Transaction
    suspend fun markAllEventsSynced(ids: List<String>) {
        ids.chunked(MAX_IDS_PER_QUERY).forEach { markEventsSynced(it) }
    }

    @Transaction
    suspend fun markAllReadsSynced(ids: List<String>) {
        ids.chunked(MAX_IDS_PER_QUERY).forEach { markReadsSynced(it) }
    }

    // Why: the feed of another account, or of none, never stays on the device.
    @Transaction
    suspend fun clear() {
        deleteAllEvents()
        deleteAllReads()
        deleteState()
        deleteBackfill()
    }

    @Query("DELETE FROM activity_event")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM activity_read")
    suspend fun deleteAllReads()

    @Query("DELETE FROM activity_state")
    suspend fun deleteState()

    @Query("DELETE FROM activity_backfill")
    suspend fun deleteBackfill()
}
