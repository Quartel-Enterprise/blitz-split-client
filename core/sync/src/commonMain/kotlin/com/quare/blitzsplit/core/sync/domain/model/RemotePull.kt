package com.quare.blitzsplit.core.sync.domain.model

/*
 * Why: what a batch of changes heard in real time pulls. Most touch one group, whose rows come down alone, with
 * Atividade's new events; what takes something off the device (a member gone, a group deleted, a guest merged into an
 * account) needs the full sync, the only one that drops what the server no longer has. An event already on the device
 * still pulls: a sync brings Atividade after the rows, so the event may be there while the rows it tells of aren't,
 * and seq follows the inserts, not the commits.
 */
data class RemotePull(
    val isFullSync: Boolean,
    val groupIds: Set<String>,
    val pullsActivity: Boolean,
    val pullsComments: Boolean,
    val pullsAttachments: Boolean,
    val pullsOwnProfile: Boolean,
    val pullsMemberPhotos: Boolean,
    val pullsMemberKeys: Boolean,
) {
    companion object {
        fun of(
            changes: List<RemoteChange>,
            userId: String,
        ): RemotePull {
            val events = changes.filterIsInstance<RemoteChange.GroupEvent>()
            val profileIds = changes.filterIsInstance<RemoteChange.ProfileChanged>().map { it.userId }.toSet()
            val reaches = events.map { RemoteReach.of(it.kind) }.toSet()
            val hasNewMember = RemoteReach.MEMBERS in reaches
            return RemotePull(
                isFullSync = RemoteReach.EVERYTHING in reaches,
                groupIds = events
                    .filter { RemoteReach.of(it.kind) in RemoteReach.groupReaches }
                    .map { it.groupId }
                    .toSet(),
                pullsActivity = events.isNotEmpty(),
                pullsComments = RemoteReach.COMMENTS in reaches,
                pullsAttachments = RemoteReach.ATTACHMENTS in reaches,
                pullsOwnProfile = userId in profileIds,
                pullsMemberPhotos = hasNewMember || profileIds.any { it != userId },
                pullsMemberKeys = hasNewMember,
            )
        }
    }
}
