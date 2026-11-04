package com.quare.blitzsplit.core.sync.domain.model

/*
 * Why: what a batch of changes heard in real time pulls. Most touch one group, whose rows come down alone, with
 * Atividade's new events; what takes something off the device (a member gone, a group deleted, a guest merged into an
 * account) or brings a group in needs the full sync, the only one that drops what the server no longer has.
 */
data class RemotePull(
    val isFullSync: Boolean,
    val groupIds: Set<String>,
    val pullsActivity: Boolean,
    val pullsComments: Boolean,
    val pullsAttachments: Boolean,
    val pullsOwnProfile: Boolean,
    val pullsMemberPhotos: Boolean,
) {
    companion object {
        /*
         * Why: an event up to newestSeq is already on the device: this device's own, heard back after the sync that
         * pushed it brought it down, or one a sync already brought.
         */
        fun of(
            changes: List<RemoteChange>,
            newestSeq: Long,
            userId: String,
        ): RemotePull {
            val events = changes.filterIsInstance<RemoteChange.GroupEvent>().filter { it.seq > newestSeq }
            val profileIds = changes.filterIsInstance<RemoteChange.ProfileChanged>().map { it.userId }.toSet()
            val reaches = events.map { RemoteReach.of(it.kind) }.toSet()
            return RemotePull(
                isFullSync = changes.any { it is RemoteChange.Joined } || RemoteReach.EVERYTHING in reaches,
                groupIds = events.filter { RemoteReach.of(it.kind) == RemoteReach.GROUP }.map { it.groupId }.toSet(),
                pullsActivity = events.isNotEmpty(),
                pullsComments = RemoteReach.COMMENTS in reaches,
                pullsAttachments = RemoteReach.ATTACHMENTS in reaches,
                pullsOwnProfile = userId in profileIds,
                pullsMemberPhotos = profileIds.any { it != userId },
            )
        }
    }
}
