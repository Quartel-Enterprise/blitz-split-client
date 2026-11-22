package com.quare.blitzsplit.shared.push

import com.quare.blitzsplit.core.activity.domain.usecase.FindActivityEvent
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/*
 * Why: an event's push syncs as it arrives, so its tap finds the bill on the device (AppViewModel.openPushLink).
 * Nothing syncs for a push without an event (the reminder) nor for one whose event is already here: a sync for an
 * earlier push of a burst, or real time with the app in front, brought it, and a second full sync would only hold
 * the next notification back.
 */
class PushArrivalSync(
    private val syncNow: SyncNow,
    private val findActivityEvent: FindActivityEvent,
) {
    suspend operator fun invoke(
        eventId: String?,
        limit: Duration,
    ): PushArrivalOutcome = when {
        eventId.isNullOrEmpty() || findActivityEvent(eventId) != null -> PushArrivalOutcome.NOTHING_NEW
        syncNow.isSyncedWithin(limit) -> PushArrivalOutcome.SYNCED
        else -> PushArrivalOutcome.FAILED
    }
}

// Why: a sync the platform gives a few seconds to (a push's wake, the watch's background refresh) stops in time.
suspend fun SyncNow.isSyncedWithin(limit: Duration): Boolean = withTimeoutOrNull(limit) { invoke() }?.isSuccess == true
