package com.quare.blitzsplit.core.activity.domain.model

// Why: everything the feed draws from, in one emission, so the items, the badge and the states never disagree.
data class ActivityFeed(
    val events: List<ActivityEvent>,
    val queued: List<QueuedChange>,
    val readState: ActivityReadState,
    val targets: ActivityTargets,
    // Why: the account of this device, whose events are read from the start; null before the first sync.
    val myUserId: String?,
    val hasOlder: Boolean,
) {
    val unreadCount: Int
        get() = events.count { readState.isUnread(it, myUserId) }
}
