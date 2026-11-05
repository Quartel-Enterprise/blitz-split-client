package com.quare.blitzsplit.core.activity.domain.model

import com.quare.blitzsplit.core.model.NotificationType

/*
 * Why: what the user read: everything up to readThroughSeq ("Marcar tudo como lido") and each event opened since.
 * Their own events, on any of their devices, are read from the start.
 */
data class ActivityReadState(
    val readThroughSeq: Long,
    val readEventIds: Set<String>,
    // Why: the feed came down at least once, so an empty one means nothing happened rather than not synced yet.
    val hasDownloaded: Boolean,
) {
    fun isUnread(
        event: ActivityEvent,
        myUserId: String?,
    ): Boolean {
        val seq = event.seq ?: return false
        return event.actorUserId != myUserId && seq > readThroughSeq && event.id !in readEventIds
    }

    // Why: Activity's dot and the bell's badge alike; a row of Notifications whose In app is off is born read.
    fun isUnread(
        event: ActivityEvent,
        myUserId: String?,
        inAppTypes: Set<NotificationType>,
    ): Boolean = event.kind.notificationType in inAppTypes && isUnread(event, myUserId)

    companion object {
        val initial: ActivityReadState = ActivityReadState(
            readThroughSeq = 0,
            readEventIds = emptySet(),
            hasDownloaded = false,
        )
    }
}
