package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomColorDao {
    // Why: the palette's swatches, in the order they were added.
    @Query("SELECT * FROM custom_color ORDER BY addedAt, color")
    fun observeAll(): Flow<List<CustomColorEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM custom_color WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT * FROM custom_color")
    suspend fun getAll(): List<CustomColorEntity>

    @Query("SELECT * FROM custom_color WHERE color = :color")
    suspend fun getByColor(color: String): CustomColorEntity?

    // Why: what goes up for this user: their changes waiting, and those made before the account, which become theirs.
    @Query("SELECT * FROM custom_color WHERE pendingSync = 1 AND (ownerUserId IS NULL OR ownerUserId = :userId)")
    suspend fun getPending(userId: String): List<CustomColorEntity>

    @Upsert
    suspend fun upsert(entities: List<CustomColorEntity>)

    // Why: a change made while the push was in flight keeps the color pending.
    @Query("UPDATE custom_color SET pendingSync = 0 WHERE color = :color AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        color: String,
        syncedUpdatedAt: Long,
    )

    // Why: colors added before the account belong to the first user a sync runs under.
    @Query("UPDATE custom_color SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    // Why: another user's palette, after a switch, never goes up as this user's.
    @Query("DELETE FROM custom_color WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersColors(ownerUserId: String)

    @Query("DELETE FROM custom_color")
    suspend fun deleteAll()
}
