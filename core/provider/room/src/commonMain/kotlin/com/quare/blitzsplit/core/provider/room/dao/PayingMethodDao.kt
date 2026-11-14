package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity
import kotlinx.coroutines.flow.Flow

// Why: the person's payment methods and the bills they marked with them, both theirs alone.
@Dao
interface PayingMethodDao {
    @Query("SELECT * FROM paying_method WHERE isDeleted = 0 ORDER BY addedAt, id")
    fun observeLiveMethods(): Flow<List<PayingMethodEntity>>

    @Query("SELECT * FROM paying_method")
    suspend fun getAllMethods(): List<PayingMethodEntity>

    @Upsert
    suspend fun upsertMethods(entities: List<PayingMethodEntity>)

    // Why: a change made while the push was in flight keeps the method pending.
    @Query("UPDATE paying_method SET pendingSync = 0 WHERE id = :id AND updatedAt = :syncedUpdatedAt")
    suspend fun markMethodSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    @Query("DELETE FROM paying_method WHERE isDeleted = 1 AND pendingSync = 0")
    suspend fun deleteSyncedTombstones()

    @Query("SELECT * FROM bill_paying_method WHERE methodId IS NOT NULL")
    fun observeMarks(): Flow<List<BillPayingMethodEntity>>

    @Query("SELECT * FROM bill_paying_method")
    suspend fun getAllMarks(): List<BillPayingMethodEntity>

    @Query("SELECT * FROM bill_paying_method WHERE expenseId = :expenseId")
    suspend fun getMark(expenseId: String): BillPayingMethodEntity?

    @Upsert
    suspend fun upsertMarks(entities: List<BillPayingMethodEntity>)

    /*
     * Why: only marks whose bill is already on the server go up, which the server needs to take them; one whose bill
     * still waits goes in a later sync.
     */
    @Query(
        "SELECT m.* FROM bill_paying_method m JOIN expense e ON e.id = m.expenseId " +
            "WHERE m.pendingSync = 1 AND e.pendingSync = 0",
    )
    suspend fun getMarksReadyToUpload(): List<BillPayingMethodEntity>

    @Query(
        "UPDATE bill_paying_method SET pendingSync = 0 WHERE expenseId = :expenseId AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markMarkSynced(
        expenseId: String,
        syncedUpdatedAt: Long,
    )

    // Why: a bill gone from the device (its group left or deleted) takes its mark along, never left waiting to go up.
    @Query("DELETE FROM bill_paying_method WHERE expenseId NOT IN (SELECT id FROM expense)")
    suspend fun deleteOrphanMarks()

    // Why: the same rule as getMarksReadyToUpload, so a mark whose bill waits never keeps a sync looping.
    @Query(
        "SELECT EXISTS(SELECT 1 FROM paying_method WHERE pendingSync = 1) OR EXISTS(SELECT 1 FROM " +
            "bill_paying_method m JOIN expense e ON e.id = m.expenseId WHERE m.pendingSync = 1 AND e.pendingSync = 0)",
    )
    fun observeHasPending(): Flow<Boolean>

    // Why: another user's methods and marks, after a switch, never go up as this user's.
    @Query("DELETE FROM paying_method WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersMethods(ownerUserId: String)

    @Query("DELETE FROM bill_paying_method WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersMarks(ownerUserId: String)

    // Why: what was added before the account belongs to the first user a sync runs under.
    @Query("UPDATE paying_method SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnownedMethods(ownerUserId: String)

    @Query("UPDATE bill_paying_method SET ownerUserId = :ownerUserId WHERE ownerUserId IS NULL")
    suspend fun claimUnownedMarks(ownerUserId: String)

    @Query("DELETE FROM paying_method")
    suspend fun deleteAllMethods()

    @Query("DELETE FROM bill_paying_method")
    suspend fun deleteAllMarks()
}
