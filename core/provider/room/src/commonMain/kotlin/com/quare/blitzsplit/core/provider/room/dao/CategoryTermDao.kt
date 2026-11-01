package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryTermDao {
    @Query("SELECT * FROM category_term ORDER BY addedAt, termKey")
    fun observeAll(): Flow<List<CategoryTermEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM category_term WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT * FROM category_term")
    suspend fun getAll(): List<CategoryTermEntity>

    // Why: what goes up for this user: their changes waiting, and those made before the account, which become theirs.
    @Query("SELECT * FROM category_term WHERE pendingSync = 1 AND (ownerUserId IS NULL OR ownerUserId = :userId)")
    suspend fun getPending(userId: String): List<CategoryTermEntity>

    @Upsert
    suspend fun upsert(entities: List<CategoryTermEntity>)

    // Why: a change made while the push was in flight keeps the term pending.
    @Query("UPDATE category_term SET pendingSync = 0 WHERE termKey = :termKey AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        termKey: String,
        syncedUpdatedAt: Long,
    )

    // Why: changes made before the account belong to the first user a sync runs under.
    @Query("UPDATE category_term SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    // Why: another user's map, after a switch, never goes up as this user's.
    @Query("DELETE FROM category_term WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersTerms(ownerUserId: String)

    @Query("DELETE FROM category_term")
    suspend fun deleteAll()
}
