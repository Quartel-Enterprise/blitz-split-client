package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentKeyDao {
    @Query("SELECT * FROM payment_key WHERE isDeleted = 0 ORDER BY addedAt, id")
    fun observeLive(): Flow<List<PaymentKeyEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM payment_key WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT * FROM payment_key")
    suspend fun getAll(): List<PaymentKeyEntity>

    @Upsert
    suspend fun upsert(entities: List<PaymentKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: PaymentKeyEntity)

    // Why: a change made while the push was in flight keeps the key pending.
    @Query("UPDATE payment_key SET pendingSync = 0 WHERE id = :id AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    // Why: once its deletion is up, a tombstone has nothing left to tell.
    @Query("DELETE FROM payment_key WHERE isDeleted = 1 AND pendingSync = 0")
    suspend fun deleteSyncedTombstones()

    // Why: Last-Write-Wins, and a local change made offline never loses to an older server value.
    @Query(
        "UPDATE payment_key SET type = :type, value = :value, isDefault = :isDefault, isDeleted = :isDeleted, " +
            "addedAt = :addedAt, updatedAt = :remoteUpdatedAt WHERE id = :id AND ownerUserId = :ownerUserId " +
            "AND pendingSync = 0 AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun applyRemote(
        id: String,
        type: String,
        value: String,
        isDefault: Boolean,
        isDeleted: Boolean,
        addedAt: Long,
        remoteUpdatedAt: Long,
        ownerUserId: String,
    )

    // Why: keys added before the account belong to the first user a sync runs under.
    @Query("UPDATE payment_key SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    // Why: another user's keys, after a switch, never go up as this user's, even ones not pushed yet.
    @Query("DELETE FROM payment_key WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersKeys(ownerUserId: String)

    @Query("DELETE FROM payment_key")
    suspend fun deleteAll()
}
