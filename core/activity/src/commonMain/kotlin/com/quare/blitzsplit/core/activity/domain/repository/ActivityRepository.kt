package com.quare.blitzsplit.core.activity.domain.repository

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    // Why: the device's 90 days, then the older pages loaded this session, the most recent first.
    fun observeEvents(): Flow<List<ActivityEvent>>

    fun observeQueued(): Flow<List<QueuedChange>>

    fun observeReadState(): Flow<ActivityReadState>

    fun observeTargets(): Flow<ActivityTargets>

    // Why: false once a page older than the device's came back short: the feed reached the group's beginning.
    fun observeHasOlder(): Flow<Boolean>

    suspend fun saveRead(eventId: String): Result<Unit>

    suspend fun saveReadThrough(
        throughSeq: Long,
        now: Long,
    ): Result<Unit>

    suspend fun saveCharges(
        charges: List<ChargeSent>,
        actorUserId: String?,
        sentAt: Long,
    ): Result<Unit>

    // Why: online only; the page before the oldest event the feed has.
    suspend fun loadOlder(): Result<Unit>

    suspend fun uploadPendingActivity(userId: String): Result<Unit>

    suspend fun downloadActivity(userId: String): Result<Unit>

    suspend fun clear()
}
