package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.AppInviteEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInviteDao {
    @Query("SELECT * FROM app_invite WHERE id = '$SINGLE_ROW_ID'")
    fun observe(): Flow<AppInviteEntity?>

    @Query("SELECT * FROM app_invite WHERE id = '$SINGLE_ROW_ID'")
    suspend fun get(): AppInviteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppInviteEntity)

    // Why: a dismissal made while the push was in flight keeps the row pending.
    @Query("UPDATE app_invite SET pendingSync = 0 WHERE id = '$SINGLE_ROW_ID' AND dismissedAt = :syncedDismissedAt")
    suspend fun markSynced(syncedDismissedAt: Long)

    // Why: a dismissal made before the account belongs to the first user a sync runs under.
    @Query("UPDATE app_invite SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    // Why: another user's row, after a switch, gives way to this user's, even a dismissal not pushed yet.
    @Query("DELETE FROM app_invite WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsers(ownerUserId: String)

    @Query("DELETE FROM app_invite")
    suspend fun deleteAll()
}
