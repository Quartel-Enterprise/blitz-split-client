package com.quare.blitzsplit.core.activity.domain.model

import com.quare.blitzsplit.core.model.NotificationType

// Why: everything the feed draws from, in one emission, so the items, the badge and the states never disagree.
data class ActivityFeed(
    val events: List<ActivityEvent>,
    val queued: List<QueuedChange>,
    val readState: ActivityReadState,
    val targets: ActivityTargets,
    // Why: the account of this device, whose events are read from the start; null before the first sync.
    val myUserId: String?,
    val hasOlder: Boolean,
    // Why: the rows of Notifications whose In app toggle is on; the others' events show in the feed already read.
    val inAppTypes: Set<NotificationType>,
) {
    val unreadCount: Int
        get() = events.count(::isUnread)

    fun isUnread(event: ActivityEvent): Boolean = readState.isUnread(event, myUserId, inAppTypes)
}
