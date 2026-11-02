package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
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

    @Query("SELECT EXISTS(SELECT 1 FROM expense WHERE id = :id)")
    suspend fun hasBill(id: String): Boolean

    @Insert
    suspend fun insertComment(entity: BillCommentEntity)

    // Why: a comment is never edited, so the server's copy (its author and thread as stamped there) replaces the device's.
    @Transaction
    suspend fun applyRemoteToBills(entities: List<BillCommentEntity>) {
        upsertComments(entities.filter { hasBill(it.expenseId) })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(entities: List<BillCommentEntity>)

    @Query("UPDATE bill_comment SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

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
