package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_COMMENT_IDS_PER_QUERY = 500

@Dao
interface BillCommentDao {
    // Why: in the order they were written; the threads are put together in memory.
    @Query("SELECT * FROM bill_comment WHERE expenseId = :expenseId ORDER BY postedAt, id")
    fun observeComments(expenseId: String): Flow<List<BillCommentEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bill_comment WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT id FROM bill_comment WHERE pendingSync = 1 ORDER BY postedAt, id")
    fun observePendingIds(): Flow<List<String>>

    // Why: in the order they were written, so a reply never goes up before the comment it answers.
    @Query(
        "SELECT * FROM bill_comment WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId) ORDER BY postedAt, id",
    )
    suspend fun getPendingComments(ownerUserId: String): List<BillCommentEntity>

    @Query(
        "SELECT id FROM bill_comment WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedCommentIds(ownerUserId: String): List<String>

    // Why: a bill a sync took off in the meantime takes no comment, which would stay pending with nowhere to go up.
    @Transaction
    suspend fun insertCommentOnBill(entity: BillCommentEntity) {
        check(hasBill(entity.expenseId)) { "No bill ${entity.expenseId}" }
        insertComment(entity)
    }

    // Why: the author's edit, kept on the device first, offline too; a deleted comment stays deleted.
    @Query(
        "UPDATE bill_comment SET body = :body, editedAt = :editedAt, pendingSync = 1 " +
            "WHERE id = :id AND isDeleted = 0",
    )
    suspend fun editComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Int

    // Why: a tombstone with no text, which the sync takes up; its replies stay.
    @Query("UPDATE bill_comment SET body = '', isDeleted = 1, pendingSync = 1 WHERE id = :id")
    suspend fun deleteComment(id: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM expense WHERE id = :id)")
    suspend fun hasBill(id: String): Boolean

    @Insert
    suspend fun insertComment(entity: BillCommentEntity)

    /*
     * Why: the server's copy (its author and thread as stamped there) replaces the device's, but for one edited or
     * deleted here and not up yet, whose change would be lost before it goes up.
     */
    @Transaction
    suspend fun applyRemoteToBills(entities: List<BillCommentEntity>) {
        val pendingIds = getPendingIds().toSet()
        upsertComments(entities.filter { it.id !in pendingIds && hasBill(it.expenseId) })
    }

    @Query("SELECT id FROM bill_comment WHERE pendingSync = 1")
    suspend fun getPendingIds(): List<String>

    // Why: an upsert, never REPLACE, whose delete and insert would cascade to the comment's likes (pending ones too).
    @Upsert
    suspend fun upsertComments(entities: List<BillCommentEntity>)

    // Why: only the version that went up; one edited again meanwhile still waits.
    @Transaction
    suspend fun markSynced(entities: List<BillCommentEntity>) {
        entities.forEach { markSynced(it.id, it.body, it.isDeleted) }
    }

    @Query("UPDATE bill_comment SET pendingSync = 0 WHERE id = :id AND body = :body AND isDeleted = :isDeleted")
    suspend fun markSynced(
        id: String,
        body: String,
        isDeleted: Boolean,
    )

    // Why: the server answers every comment of the user's groups, so a synced one missing there was dropped there.
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCommentIds: Set<String>,
    ) {
        getSyncedCommentIds(ownerUserId)
            .filterNot { it in serverCommentIds }
            .chunked(MAX_COMMENT_IDS_PER_QUERY)
            .forEach { deleteComments(it) }
    }

    @Query("DELETE FROM bill_comment WHERE id IN (:ids)")
    suspend fun deleteComments(ids: List<String>)
}
