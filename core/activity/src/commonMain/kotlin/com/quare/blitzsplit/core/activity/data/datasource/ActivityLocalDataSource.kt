package com.quare.blitzsplit.core.activity.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityStateEntity
import com.quare.blitzsplit.core.provider.room.entity.QueuedBillRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedGroupRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedPaymentRow
import kotlinx.coroutines.flow.Flow

interface ActivityLocalDataSource {
    fun observeEvents(): Flow<List<ActivityEventEntity>>

    fun observeReadEventIds(): Flow<List<String>>

    fun observeState(): Flow<ActivityStateEntity?>

    fun observeQueuedBills(): Flow<List<QueuedBillRow>>

    fun observeQueuedPayments(): Flow<List<QueuedPaymentRow>>

    fun observeQueuedGroups(): Flow<List<QueuedGroupRow>>

    fun observeLiveBillIds(): Flow<List<String>>

    fun observeLiveGroupIds(): Flow<List<String>>

    suspend fun getState(): ActivityStateEntity?

    suspend fun getMaxSeq(): Long?

    suspend fun getOldestSeqOrNull(): Long?

    suspend fun getPendingEvents(): List<ActivityEventEntity>

    suspend fun getPendingReadIds(): List<String>

    suspend fun getGroupIdsToBackfill(): List<String>

    suspend fun markBackfilled(groupIds: List<String>)

    suspend fun upsertEvents(entities: List<ActivityEventEntity>)

    suspend fun insertEvents(entities: List<ActivityEventEntity>)

    suspend fun markRead(eventId: String)

    suspend fun insertSyncedReads(eventIds: List<String>)

    suspend fun markAllRead(
        throughSeq: Long,
        now: Long,
    )

    suspend fun applyRemoteMark(throughSeq: Long)

    suspend fun markEventsSynced(ids: List<String>)

    suspend fun markReadsSynced(ids: List<String>)

    suspend fun markMarkSynced(syncedUpdatedAt: Long)

    suspend fun markDownloaded(userId: String)

    suspend fun prune(cutoff: Long)

    suspend fun clear()
}
