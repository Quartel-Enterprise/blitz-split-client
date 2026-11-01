package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupPinDao {
    @Query("SELECT groupId FROM group_pin WHERE isPinned = 1")
    fun observePinnedGroupIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM group_pin WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT * FROM group_pin WHERE pendingSync = 1")
    suspend fun getPending(): List<GroupPinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GroupPinEntity)

    // Why: a change made while the push was in flight keeps the pin pending.
    @Query("UPDATE group_pin SET pendingSync = 0 WHERE groupId = :groupId AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        groupId: String,
        syncedUpdatedAt: Long,
    )

    /*
     * Why: Last-Write-Wins as the server keeps it: a server pin is taken when the device has none for the group, or an
     * older one that isn't waiting to go up. A pin of a group not on the device yet waits for the next download.
     */
    @Transaction
    suspend fun applyRemote(
        groupId: String,
        isPinned: Boolean,
        remoteUpdatedAt: Long,
    ) {
        insertRemoteIfAbsent(groupId, isPinned, remoteUpdatedAt)
        updateFromRemote(groupId, isPinned, remoteUpdatedAt)
    }

    @Query(
        "INSERT OR IGNORE INTO group_pin (groupId, isPinned, updatedAt, pendingSync) " +
            "SELECT :groupId, :isPinned, :remoteUpdatedAt, 0 WHERE EXISTS (SELECT 1 FROM split_group WHERE id = :groupId)",
    )
    suspend fun insertRemoteIfAbsent(
        groupId: String,
        isPinned: Boolean,
        remoteUpdatedAt: Long,
    )

    @Query(
        "UPDATE group_pin SET isPinned = :isPinned, updatedAt = :remoteUpdatedAt " +
            "WHERE groupId = :groupId AND pendingSync = 0 AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateFromRemote(
        groupId: String,
        isPinned: Boolean,
        remoteUpdatedAt: Long,
    )

    @Query("SELECT updatedAt FROM group_pin WHERE groupId = :groupId")
    suspend fun getUpdatedAt(groupId: String): Long?
}
