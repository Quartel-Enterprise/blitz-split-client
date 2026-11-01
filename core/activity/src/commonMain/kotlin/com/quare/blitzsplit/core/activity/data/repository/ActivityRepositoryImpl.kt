package com.quare.blitzsplit.core.activity.data.repository

import com.quare.blitzsplit.core.activity.data.datasource.ActivityLocalDataSource
import com.quare.blitzsplit.core.activity.data.datasource.ActivityRemoteDataSource
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadMarkDto
import com.quare.blitzsplit.core.activity.data.mapper.ActivityMapper
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

class ActivityRepositoryImpl(
    private val local: ActivityLocalDataSource,
    private val remote: ActivityRemoteDataSource,
    private val mapper: ActivityMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : ActivityRepository {
    // Why: the device keeps the last 90 days to read offline.
    private val keptFor = 90.days

    /*
     * Why: the pages older than the device keeps live in memory, for this session only: the next sync's pruning would
     * otherwise take them off the list while it is being read.
     */
    private val olderEvents = MutableStateFlow<List<ActivityEvent>>(emptyList())
    private val hasOlder = MutableStateFlow(true)

    override fun observeEvents(): Flow<List<ActivityEvent>> = combine(
        local.observeEvents().map { entities -> entities.mapNotNull(mapper::toDomain) },
        olderEvents,
    ) { recent, older ->
        val recentIds = recent.mapTo(HashSet()) { it.id }
        recent + older.filterNot { it.id in recentIds }
    }.distinctUntilChanged()

    override fun observeQueued(): Flow<List<QueuedChange>> = combine(
        local.observeQueuedBills(),
        local.observeQueuedPayments(),
        local.observeQueuedGroups(),
    ) { bills, payments, groups ->
        (bills.map(mapper::toQueued) + payments.map(mapper::toQueued) + groups.map(mapper::toQueued))
            .sortedByDescending { it.savedAt }
    }.distinctUntilChanged()

    override fun observeReadState(): Flow<ActivityReadState> = combine(
        local.observeState(),
        local.observeReadEventIds(),
    ) { state, readIds ->
        ActivityReadState(
            readThroughSeq = state?.readThroughSeq ?: 0,
            readEventIds = readIds.toSet(),
            hasDownloaded = state?.hasDownloaded ?: false,
        )
    }.distinctUntilChanged()

    override fun observeTargets(): Flow<ActivityTargets> = combine(
        local.observeLiveBillIds(),
        local.observeLiveGroupIds(),
    ) { billIds, groupIds ->
        ActivityTargets(
            billIds = billIds.toSet(),
            groupIds = groupIds.toSet(),
        )
    }.distinctUntilChanged()

    override fun observeHasOlder(): Flow<Boolean> = hasOlder

    override suspend fun saveRead(eventId: String): Result<Unit> = suspendRunCatching { local.markRead(eventId) }

    override suspend fun saveReadThrough(
        throughSeq: Long,
        now: Long,
    ): Result<Unit> = suspendRunCatching {
        local.markAllRead(
            throughSeq = throughSeq,
            now = now,
        )
    }

    override suspend fun saveCharges(
        charges: List<ChargeSent>,
        actorUserId: String?,
        sentAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.insertEvents(
            charges.map { charge ->
                mapper.toEntity(
                    charge = charge,
                    id = Uuid.random().toString(),
                    actorUserId = actorUserId,
                    sentAt = sentAt,
                )
            },
        )
    }

    override suspend fun loadOlder(): Result<Unit> = suspendRunCatching {
        val oldestSeq = (local.getOldestSeqOrNull() to olderEvents.value.mapNotNull { it.seq }.minOrNull())
            .let { (recent, older) -> listOfNotNull(recent, older).minOrNull() }
        if (oldestSeq == null) {
            hasOlder.value = false
            return@suspendRunCatching
        }
        val page = remote.fetchEventsBefore(
            beforeSeq = oldestSeq,
            limit = OLDER_PAGE_SIZE,
        )
        olderEvents.update { loaded -> loaded + page.map(mapper::toEntity).mapNotNull(mapper::toDomain) }
        if (page.size < OLDER_PAGE_SIZE) hasOlder.value = false
    }

    // Why: before the download, so the charges sent here come back in the same sync with the server's seq.
    override suspend fun uploadPendingActivity(userId: String): Result<Unit> = suspendRunCatching {
        val charges = local.getPendingEvents().filter { it.kind == ActivityKind.CHARGE_SENT.code }
        if (charges.isNotEmpty()) {
            remote.insertCharges(charges.map { mapper.toChargeDto(it, userId) })
            local.markEventsSynced(charges.map { it.id })
        }
        val readIds = local.getPendingReadIds()
        if (readIds.isNotEmpty()) {
            remote.insertReads(
                readIds.map {
                    ActivityReadDto(
                        userId = userId,
                        eventId = it,
                    )
                },
            )
            local.markReadsSynced(readIds)
        }
        val state = local.getState()
        if (state != null && state.markPendingSync) {
            remote.upsertReadMark(
                ActivityReadMarkDto(
                    userId = userId,
                    readThroughSeq = state.readThroughSeq,
                    updatedAt = state.markUpdatedAt,
                ),
            )
            local.markMarkSynced(state.markUpdatedAt)
        }
    }

    /*
     * Why: what happened since the newest event the device has, then the history of the groups new on it (joined, or
     * come down on another account's first sync), the last 90 days only. Another account's feed goes first.
     */
    override suspend fun downloadActivity(userId: String): Result<Unit> = suspendRunCatching {
        val owner = local.getState()?.ownerUserId
        if (owner != null && owner != userId) {
            local.clear()
            olderEvents.value = emptyList()
            hasOlder.value = true
        }
        remote.fetchReadMark()?.let { local.applyRemoteMark(it.readThroughSeq) }
        local.insertSyncedReads(remote.fetchReadEventIds())
        val since = currentTimeMillis() - keptFor.inWholeMilliseconds
        val cursor = local.getMaxSeq() ?: 0L
        val recent = remote.fetchEventsAfter(
            afterSeq = cursor,
            since = since,
        )
        local.upsertEvents(recent.map(mapper::toEntity))
        val newGroupIds = local.getGroupIdsToBackfill()
        // Why: the first download already brought every group's history.
        if (cursor > 0 && newGroupIds.isNotEmpty()) {
            val history = remote.fetchGroupEvents(
                groupIds = newGroupIds,
                throughSeq = cursor,
                since = since,
            )
            local.upsertEvents(history.map(mapper::toEntity))
        }
        local.markBackfilled(newGroupIds)
        local.prune(since)
        local.markDownloaded(userId)
    }

    override suspend fun clear() {
        local.clear()
        olderEvents.value = emptyList()
        hasOlder.value = true
    }

    private companion object {
        const val OLDER_PAGE_SIZE = 50L
    }
}
