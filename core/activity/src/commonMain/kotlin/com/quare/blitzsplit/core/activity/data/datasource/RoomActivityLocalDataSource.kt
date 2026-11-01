package com.quare.blitzsplit.core.activity.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ActivityDao
import com.quare.blitzsplit.core.provider.room.entity.ActivityBackfillEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityReadEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityStateEntity
import com.quare.blitzsplit.core.provider.room.entity.QueuedBillRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedGroupRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedPaymentRow
import kotlinx.coroutines.flow.Flow

class RoomActivityLocalDataSource(
    private val dao: ActivityDao,
) : ActivityLocalDataSource {
    override fun observeEvents(): Flow<List<ActivityEventEntity>> = dao.observeEvents()

    override fun observeReadEventIds(): Flow<List<String>> = dao.observeReadEventIds()

    override fun observeState(): Flow<ActivityStateEntity?> = dao.observeState()

    override fun observeQueuedBills(): Flow<List<QueuedBillRow>> = dao.observeQueuedBills()

    override fun observeQueuedPayments(): Flow<List<QueuedPaymentRow>> = dao.observeQueuedPayments()

    override fun observeQueuedGroups(): Flow<List<QueuedGroupRow>> = dao.observeQueuedGroups()

    override fun observeLiveBillIds(): Flow<List<String>> = dao.observeLiveBillIds()

    override fun observeLiveGroupIds(): Flow<List<String>> = dao.observeLiveGroupIds()

    override suspend fun getState(): ActivityStateEntity? = dao.getState()

    override suspend fun getMaxSeq(): Long? = dao.getMaxSeq()

    override suspend fun getOldestSeqOrNull(): Long? = dao.getMinSeq()

    override suspend fun getPendingEvents(): List<ActivityEventEntity> = dao.getPendingEvents()

    override suspend fun getPendingReadIds(): List<String> = dao.getPendingReadIds()

    override suspend fun getGroupIdsToBackfill(): List<String> = dao.getGroupIdsToBackfill()

    override suspend fun markBackfilled(groupIds: List<String>) {
        dao.insertBackfilled(groupIds.map(::ActivityBackfillEntity))
    }

    override suspend fun upsertEvents(entities: List<ActivityEventEntity>) {
        dao.upsertEvents(entities)
    }

    override suspend fun insertEvents(entities: List<ActivityEventEntity>) {
        dao.insertEvents(entities)
    }

    override suspend fun markRead(eventId: String) {
        dao.insertReads(
            listOf(
                ActivityReadEntity(
                    eventId = eventId,
                    pendingSync = true,
                ),
            ),
        )
    }

    override suspend fun insertSyncedReads(eventIds: List<String>) {
        dao.insertReads(
            eventIds.map {
                ActivityReadEntity(
                    eventId = it,
                    pendingSync = false,
                )
            },
        )
    }

    override suspend fun markAllRead(
        throughSeq: Long,
        now: Long,
    ) {
        dao.markAllRead(throughSeq, now)
    }

    override suspend fun applyRemoteMark(throughSeq: Long) {
        dao.applyRemoteMark(throughSeq)
    }

    override suspend fun markEventsSynced(ids: List<String>) {
        dao.markAllEventsSynced(ids)
    }

    override suspend fun markReadsSynced(ids: List<String>) {
        dao.markAllReadsSynced(ids)
    }

    override suspend fun markMarkSynced(syncedUpdatedAt: Long) {
        dao.markMarkSynced(syncedUpdatedAt)
    }

    override suspend fun markDownloaded(userId: String) {
        dao.ensureState()
        dao.markDownloaded(userId)
    }

    override suspend fun prune(cutoff: Long) {
        dao.prune(cutoff)
    }

    override suspend fun clear() {
        dao.clear()
    }
}
