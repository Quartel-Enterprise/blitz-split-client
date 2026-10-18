package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = '$SINGLE_ROW_ID'")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = '$SINGLE_ROW_ID'")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)

    // Why: a change made while the push was in flight keeps the row pending.
    @Query("UPDATE profile SET pendingSync = 0 WHERE id = '$SINGLE_ROW_ID' AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(syncedUpdatedAt: Long)

    // Why: Last-Write-Wins, and a local change made offline never loses to an older server value.
    @Query(
        "UPDATE profile SET displayName = :displayName, avatarColor = :avatarColor, photoUrl = :photoUrl, " +
            "updatedAt = :remoteUpdatedAt WHERE id = '$SINGLE_ROW_ID' AND pendingSync = 0 " +
            "AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun applyRemote(
        displayName: String,
        avatarColor: String,
        photoUrl: String?,
        remoteUpdatedAt: Long,
    )

    @Query("DELETE FROM profile")
    suspend fun deleteAll()
}
