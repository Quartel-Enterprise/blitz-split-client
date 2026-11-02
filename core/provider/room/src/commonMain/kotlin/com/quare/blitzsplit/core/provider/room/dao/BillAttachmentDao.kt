package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_ATTACHMENT_IDS_PER_QUERY = 500

@Dao
interface BillAttachmentDao {
    // Why: the receipt first, then the photos in the order they were added (7.3g).
    @Query(
        "SELECT * FROM bill_attachment WHERE expenseId = :expenseId AND isDeleted = 0 " +
            "ORDER BY kind = 'receipt' DESC, addedAt, id",
    )
    fun observeAttachments(expenseId: String): Flow<List<BillAttachmentEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bill_attachment WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT id || ':' || updatedAt FROM bill_attachment WHERE pendingSync = 1 ORDER BY addedAt, id")
    fun observePendingVersions(): Flow<List<String>>

    // Why: only the ids, since each row may hold its JPEG; the sync reads them one at a time.
    @Query(
        "SELECT id FROM bill_attachment WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId) ORDER BY addedAt, id",
    )
    suspend fun getPendingAttachmentIds(ownerUserId: String): List<String>

    @Query(
        "SELECT id FROM bill_attachment WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedAttachmentIds(ownerUserId: String): List<String>

    @Query("SELECT * FROM bill_attachment WHERE id = :id")
    suspend fun getAttachment(id: String): BillAttachmentEntity?

    @Query("SELECT COUNT(*) FROM bill_attachment WHERE expenseId = :expenseId AND kind = :kind AND isDeleted = 0")
    suspend fun countAttachments(
        expenseId: String,
        kind: String,
    ): Int

    @Query("SELECT EXISTS(SELECT 1 FROM expense WHERE id = :id)")
    suspend fun hasBill(id: String): Boolean

    /*
     * Why: on a bill still on the device and while it has room for one more of its kind (5 photos, 1 receipt), checked
     * in one transaction so two quick adds never pass the limit together. False when it has no room.
     */
    @Transaction
    suspend fun insertWithinLimit(
        entity: BillAttachmentEntity,
        limit: Int,
    ): Boolean {
        check(hasBill(entity.expenseId)) { "No bill ${entity.expenseId}" }
        if (countAttachments(entity.expenseId, entity.kind) >= limit) return false
        upsertAttachments(listOf(entity))
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(entities: List<BillAttachmentEntity>)

    @Query("DELETE FROM bill_attachment WHERE id IN (:ids)")
    suspend fun deleteAttachments(ids: List<String>)

    /*
     * Why: a tombstone until it goes up, even for one added here and not synced yet: its upload may have reached the
     * server with the answer lost, and a row simply dropped would leave the photo there. Its image is no longer needed.
     */
    @Transaction
    suspend fun remove(
        id: String,
        updatedAt: Long,
    ) {
        val attachment = getAttachment(id) ?: return
        upsertAttachments(
            listOf(
                attachment.copy(
                    isDeleted = true,
                    updatedAt = maxOf(updatedAt, attachment.updatedAt + 1),
                    pendingJpeg = null,
                    pendingSync = true,
                ),
            ),
        )
    }

    /*
     * Why: the version that went up is synced; a tombstone leaves the device. A change made while it went up (taken
     * off meanwhile) has another `updatedAt` and stays pending.
     */
    @Transaction
    suspend fun markSynced(
        id: String,
        updatedAt: Long,
    ) {
        val attachment = getAttachment(id) ?: return
        if (attachment.updatedAt != updatedAt) return
        if (attachment.isDeleted) {
            deleteAttachments(listOf(id))
        } else {
            upsertAttachments(
                listOf(
                    attachment.copy(
                        pendingJpeg = null,
                        pendingSync = false,
                    ),
                ),
            )
        }
    }

    /*
     * Why: Last-Write-Wins against a change not up yet, which stays when it is newer. A tombstone takes it off the
     * device, and one of a bill no longer here is skipped.
     */
    @Transaction
    suspend fun applyRemote(entities: List<BillAttachmentEntity>) {
        val kept = entities.filter { remote ->
            val local = getAttachment(remote.id)
            local == null || !local.pendingSync || remote.isDeleted || remote.updatedAt > local.updatedAt
        }
        kept
            .filter { it.isDeleted }
            .map { it.id }
            .chunked(
                MAX_ATTACHMENT_IDS_PER_QUERY,
            ).forEach { deleteAttachments(it) }
        upsertAttachments(kept.filter { !it.isDeleted && hasBill(it.expenseId) })
    }

    // Why: the server answers every attachment of the user's groups, so a synced one missing there was dropped there.
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverAttachmentIds: Set<String>,
    ) {
        getSyncedAttachmentIds(ownerUserId)
            .filterNot { it in serverAttachmentIds }
            .chunked(MAX_ATTACHMENT_IDS_PER_QUERY)
            .forEach { deleteAttachments(it) }
    }
}
