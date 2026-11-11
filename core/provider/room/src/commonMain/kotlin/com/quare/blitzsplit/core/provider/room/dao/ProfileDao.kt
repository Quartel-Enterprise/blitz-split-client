package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
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

    /*
     * Why: an edit of the name, color or photo, in one write with the read of the link's code, so a code a sync writes
     * meanwhile (made here, or the server's) is never put back to the one read before it.
     */
    @Transaction
    suspend fun upsertKeepingShareCode(entity: ProfileEntity) {
        upsert(entity.copy(shareCode = get()?.shareCode))
    }

    // Why: a change made while the push was in flight keeps the row pending, and its photo with it.
    @Query(
        "UPDATE profile SET pendingSync = 0, pendingPhoto = NULL, photoUrl = :photoUrl " +
            "WHERE id = '$SINGLE_ROW_ID' AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    )

    // Why: Last-Write-Wins, and a local change made offline never loses to an older server value.
    @Query(
        "UPDATE profile SET displayName = :displayName, avatarColor = :avatarColor, photoUrl = :photoUrl, " +
            "updatedAt = :remoteUpdatedAt WHERE id = '$SINGLE_ROW_ID' AND ownerUserId = :ownerUserId " +
            "AND pendingSync = 0 AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun applyRemote(
        displayName: String,
        avatarColor: String,
        photoUrl: String?,
        remoteUpdatedAt: Long,
        ownerUserId: String?,
    )

    // Why: a profile made before its account belongs to the first user a sync runs under.
    @Query("UPDATE profile SET ownerUserId = :ownerUserId WHERE id = '$SINGLE_ROW_ID' AND ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    /*
     * Why: another user's copy, after a switch, gives way to the account's profile whatever its age, even a
     * change or a photo not pushed yet.
     */
    @Query(
        "UPDATE profile SET displayName = :displayName, avatarColor = :avatarColor, photoUrl = :photoUrl, " +
            "pendingPhoto = NULL, updatedAt = :remoteUpdatedAt, pendingSync = 0, ownerUserId = :ownerUserId " +
            "WHERE id = '$SINGLE_ROW_ID' AND ownerUserId != :ownerUserId",
    )
    suspend fun replaceOtherUsersCopy(
        displayName: String,
        avatarColor: String,
        photoUrl: String?,
        remoteUpdatedAt: Long,
        ownerUserId: String?,
    )

    // Why: another user's copy whose new account has no profile on the server goes up as the account's.
    @Query(
        "UPDATE profile SET pendingSync = 1, updatedAt = :updatedAt, photoUrl = :photoUrl, " +
            "ownerUserId = :ownerUserId WHERE id = '$SINGLE_ROW_ID' AND ownerUserId != :ownerUserId",
    )
    suspend fun reclaimOtherUsersCopy(
        ownerUserId: String,
        updatedAt: Long,
        photoUrl: String?,
    )

    // Why: the account's own code, made here before the server knows the profile, never another user's copy.
    @Query(
        "UPDATE profile SET shareCode = :shareCode, pendingSync = 1 " +
            "WHERE id = '$SINGLE_ROW_ID' AND ownerUserId = :ownerUserId AND shareCode IS NULL",
    )
    suspend fun createShareCode(
        ownerUserId: String,
        shareCode: String,
    )

    // Why: a code the server refused as taken gives way to a new one before the profile goes up again.
    @Query(
        "UPDATE profile SET shareCode = :shareCode WHERE id = '$SINGLE_ROW_ID' AND ownerUserId = :ownerUserId",
    )
    suspend fun replaceShareCode(
        ownerUserId: String,
        shareCode: String,
    )

    /*
     * Why: the server's code wins over any made here, whatever the Last-Write-Wins of the rest, since links already
     * shared carry it and it never changes there. Null drops another user's code from a copy that changed hands.
     */
    @Query(
        "UPDATE profile SET shareCode = :shareCode WHERE id = '$SINGLE_ROW_ID' AND ownerUserId = :ownerUserId",
    )
    suspend fun applyRemoteShareCode(
        ownerUserId: String,
        shareCode: String?,
    )

    @Query("DELETE FROM profile")
    suspend fun deleteAll()
}
