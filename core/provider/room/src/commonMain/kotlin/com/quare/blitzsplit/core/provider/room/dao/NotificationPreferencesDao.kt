package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPreferencesDao {
    @Query("SELECT * FROM notification_preferences WHERE id = '$SINGLE_ROW_ID'")
    fun observe(): Flow<NotificationPreferencesEntity?>

    @Query("SELECT * FROM notification_preferences WHERE id = '$SINGLE_ROW_ID'")
    suspend fun get(): NotificationPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationPreferencesEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: NotificationPreferencesEntity)

    // Why: a change made while the push was in flight keeps the row pending.
    @Query(
        "UPDATE notification_preferences SET pendingSync = 0 " +
            "WHERE id = '$SINGLE_ROW_ID' AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markSynced(syncedUpdatedAt: Long)

    /*
     * Why: Last-Write-Wins as the server keeps it. A newer server row wins even over a change made offline, which
     * the server would refuse as older anyway.
     */
    @Query(
        "UPDATE notification_preferences SET newExpense = :newExpense, paymentMarked = :paymentMarked, " +
            "settleReminders = :settleReminders, invites = :invites, updatedAt = :remoteUpdatedAt, pendingSync = 0 " +
            "WHERE id = '$SINGLE_ROW_ID' AND ownerUserId = :ownerUserId AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun applyRemote(
        newExpense: Boolean,
        paymentMarked: Boolean,
        settleReminders: Boolean,
        invites: Boolean,
        remoteUpdatedAt: Long,
        ownerUserId: String,
    )

    // Why: preferences changed before the account belong to the first user a sync runs under.
    @Query("UPDATE notification_preferences SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnowned(ownerUserId: String)

    // Why: another user's preferences, after a switch, give way to this user's, even ones not pushed yet.
    @Query("DELETE FROM notification_preferences WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsers(ownerUserId: String)

    @Query("DELETE FROM notification_preferences")
    suspend fun deleteAll()
}
