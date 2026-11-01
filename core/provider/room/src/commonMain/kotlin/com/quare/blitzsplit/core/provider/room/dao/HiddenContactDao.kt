package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenContactDao {
    @Query("SELECT * FROM hidden_contact")
    fun observeAll(): Flow<List<HiddenContactEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_contact WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT * FROM hidden_contact")
    suspend fun getAll(): List<HiddenContactEntity>

    @Query("SELECT * FROM hidden_contact WHERE personKey = :personKey")
    suspend fun getOrNull(personKey: String): HiddenContactEntity?

    @Upsert
    suspend fun upsert(entity: HiddenContactEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: HiddenContactEntity)

    // Why: a change made while the push was in flight stays pending.
    @Query("UPDATE hidden_contact SET pendingSync = 0 WHERE personKey = :personKey AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        personKey: String,
        syncedUpdatedAt: Long,
    )

    // Why: Last-Write-Wins, as the server keeps it: a newer server row wins, a change made here offline and newer stays.
    @Query(
        "UPDATE hidden_contact SET isHidden = :isHidden, hiddenAt = :hiddenAt, updatedAt = :remoteUpdatedAt, " +
            "pendingSync = 0 WHERE personKey = :personKey AND ownerUserId = :ownerUserId AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun applyRemote(
        personKey: String,
        isHidden: Boolean,
        hiddenAt: Long,
        remoteUpdatedAt: Long,
        ownerUserId: String,
    )

    // Why: what was hidden before the account belongs to the first user a sync runs under.
    @Query("UPDATE hidden_contact SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    // Why: another user's list, after a switch, never goes up as this user's.
    @Query("DELETE FROM hidden_contact WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersRows(ownerUserId: String)

    @Query("DELETE FROM hidden_contact")
    suspend fun deleteAll()
}
