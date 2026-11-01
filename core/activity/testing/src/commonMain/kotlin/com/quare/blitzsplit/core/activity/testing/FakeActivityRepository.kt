package com.quare.blitzsplit.core.activity.testing

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// In-memory ActivityRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeActivityRepository(
    initialEvents: List<ActivityEvent> = emptyList(),
) : ActivityRepository {
    val events = MutableStateFlow(initialEvents)
    val queued = MutableStateFlow(emptyList<QueuedChange>())
    val readState = MutableStateFlow(ActivityReadState.initial)
    val targets = MutableStateFlow(
        ActivityTargets(
            billIds = emptySet(),
            groupIds = emptySet(),
        ),
    )
    val hasOlder = MutableStateFlow(true)

    // Why: what a loadOlder call appends, as the server's next older page.
    var olderPage: List<ActivityEvent> = emptyList()
    val recordedCharges = mutableListOf<ChargeSent>()
    val chargeActorUserIds = mutableListOf<String?>()
    val chargeSentAts = mutableListOf<Long>()
    val markAllCalls = mutableListOf<Pair<Long, Long>>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var loadOlderCalls = 0
    var clearCalls = 0
    var markReadError: Throwable? = null
    var recordError: Throwable? = null
    var loadOlderError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    override fun observeEvents(): Flow<List<ActivityEvent>> = events

    override fun observeQueued(): Flow<List<QueuedChange>> = queued

    override fun observeReadState(): Flow<ActivityReadState> = readState

    override fun observeTargets(): Flow<ActivityTargets> = targets

    override fun observeHasOlder(): Flow<Boolean> = hasOlder

    override suspend fun saveRead(eventId: String): Result<Unit> {
        markReadError?.let { return Result.failure(it) }
        readState.update { it.copy(readEventIds = it.readEventIds + eventId) }
        return Result.success(Unit)
    }

    override suspend fun saveReadThrough(
        throughSeq: Long,
        now: Long,
    ): Result<Unit> {
        markAllCalls += throughSeq to now
        readState.update { it.copy(readThroughSeq = maxOf(it.readThroughSeq, throughSeq)) }
        return Result.success(Unit)
    }

    override suspend fun saveCharges(
        charges: List<ChargeSent>,
        actorUserId: String?,
        sentAt: Long,
    ): Result<Unit> {
        recordError?.let { return Result.failure(it) }
        recordedCharges += charges
        chargeActorUserIds += actorUserId
        chargeSentAts += sentAt
        return Result.success(Unit)
    }

    override suspend fun loadOlder(): Result<Unit> {
        loadOlderCalls++
        loadOlderError?.let { return Result.failure(it) }
        events.update { it + olderPage }
        if (olderPage.isEmpty()) hasOlder.value = false
        olderPage = emptyList()
        return Result.success(Unit)
    }

    override suspend fun uploadPendingActivity(userId: String): Result<Unit> {
        uploadedUserIds += userId
        return pushError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun downloadActivity(userId: String): Result<Unit> {
        downloadedUserIds += userId
        pullError?.let { return Result.failure(it) }
        readState.update { it.copy(hasDownloaded = true) }
        return Result.success(Unit)
    }

    override suspend fun clear() {
        clearCalls++
        events.value = emptyList()
        readState.value = ActivityReadState.initial
    }
}
