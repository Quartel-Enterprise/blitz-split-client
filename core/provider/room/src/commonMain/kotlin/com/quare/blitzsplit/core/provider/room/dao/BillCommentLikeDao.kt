package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillCommentLikeDao {
    // Why: a bill's likes, unliked ones out, in the order they were made.
    @Query(
        "SELECT l.* FROM bill_comment_like l JOIN bill_comment c ON c.id = l.commentId " +
            "WHERE c.expenseId = :expenseId AND l.isLiked = 1 ORDER BY l.updatedAt",
    )
    fun observeLikes(expenseId: String): Flow<List<BillCommentLikeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bill_comment_like WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    // Why: when each like waiting to go up was toggled, so a new toggle triggers a sync after a failed push.
    @Query("SELECT updatedAt FROM bill_comment_like WHERE pendingSync = 1 ORDER BY updatedAt")
    fun observePendingVersions(): Flow<List<Long>>

    @Query(
        "SELECT * FROM bill_comment_like WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getPendingLikes(ownerUserId: String): List<BillCommentLikeEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bill_comment WHERE id = :id)")
    suspend fun hasComment(id: String): Boolean

    // Why: a comment a sync took off in the meantime takes no like, which would stay pending with nowhere to go up.
    @Transaction
    suspend fun upsertMine(entity: BillCommentLikeEntity) {
        check(hasComment(entity.commentId)) { "No comment ${entity.commentId}" }
        upsertLikes(listOf(entity))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLikes(entities: List<BillCommentLikeEntity>)

    // Why: only the toggle that went up; one toggled again meanwhile still waits.
    @Query(
        "UPDATE bill_comment_like SET pendingSync = 0 " +
            "WHERE commentId = :commentId AND likerKey = :likerKey AND updatedAt = :updatedAt",
    )
    suspend fun markSynced(
        commentId: String,
        likerKey: String,
        updatedAt: Long,
    )

    @Transaction
    suspend fun markSynced(entities: List<BillCommentLikeEntity>) {
        entities.forEach { markSynced(it.commentId, it.likerKey, it.updatedAt) }
    }

    @Query(
        "DELETE FROM bill_comment_like WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun deleteSynced(ownerUserId: String)

    @Query("SELECT commentId FROM bill_comment_like WHERE pendingSync = 1")
    suspend fun getPendingCommentIds(): List<String>

    /*
     * Why: the server answers every like of the user's groups, so it replaces whatever went up before, keeping this
     * device's own toggles not up yet, and only on the comments the device has.
     */
    @Transaction
    suspend fun replaceSynced(
        ownerUserId: String,
        entities: List<BillCommentLikeEntity>,
    ) {
        deleteSynced(ownerUserId)
        val pendingMine = getPendingCommentIds().toSet()
        upsertLikes(
            entities.filter { like ->
                !(like.likerKey == BillCommentLikeEntity.ME && like.commentId in pendingMine) &&
                    hasComment(like.commentId)
            },
        )
    }
}
