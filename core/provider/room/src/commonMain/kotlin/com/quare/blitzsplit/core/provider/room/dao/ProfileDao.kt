package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.DEFERRED_UPDATED_AT
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

    // Why: a change made while the push was in flight keeps the row pending, and its photo with it.
    @Query(
        "UPDATE profile SET pendingSync = 0, pendingPhoto = NULL, photoUrl = :photoUrl " +
            "WHERE id = '$SINGLE_ROW_ID' AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    )

    /*
     * Why: Last-Write-Wins, and a local change made offline never loses to an older server value. The photo of a
     * copy deferred to the account (see deferToRemote) goes with it.
     */
    @Query(
        "UPDATE profile SET displayName = :displayName, avatarColor = :avatarColor, photoUrl = :photoUrl, " +
            "pendingPhoto = NULL, updatedAt = :remoteUpdatedAt WHERE id = '$SINGLE_ROW_ID' AND pendingSync = 0 " +
            "AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun applyRemote(
        displayName: String,
        avatarColor: String,
        photoUrl: String?,
        remoteUpdatedAt: Long,
    )

    // Why: the next pull applies the account's profile whatever its age, and nothing waits to go up over it.
    @Query("UPDATE profile SET pendingSync = 0, updatedAt = $DEFERRED_UPDATED_AT WHERE id = '$SINGLE_ROW_ID'")
    suspend fun deferToRemote()

    // Why: a deferred copy whose account has no profile on the server goes up as the account's.
    @Query(
        "UPDATE profile SET pendingSync = 1, updatedAt = :updatedAt, photoUrl = :photoUrl " +
            "WHERE id = '$SINGLE_ROW_ID' AND pendingSync = 0 AND updatedAt = $DEFERRED_UPDATED_AT",
    )
    suspend fun reclaimDeferred(
        updatedAt: Long,
        photoUrl: String?,
    )

    @Query("DELETE FROM profile")
    suspend fun deleteAll()
}
