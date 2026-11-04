package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind

/*
 * Why: what a batch of changes heard in real time pulls. Most touch one group, whose rows come down alone, with
 * Atividade's new events; what takes something off the device (a member gone, a group deleted, a guest merged into an
 * account) or brings a group in needs the full sync, the only one that drops what the server no longer has.
 */
data class RemotePull(
    val isFullSync: Boolean = false,
    val groupIds: Set<String> = emptySet(),
    val pullsActivity: Boolean = false,
    val pullsComments: Boolean = false,
    val pullsAttachments: Boolean = false,
    val pullsOwnProfile: Boolean = false,
    val pullsMemberPhotos: Boolean = false,
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
            val reaches = events.map { it.kind.reach }.toSet()
            return RemotePull(
                isFullSync = changes.any { it is RemoteChange.Joined } || Reach.EVERYTHING in reaches,
                groupIds = events.filter { it.kind.reach == Reach.GROUP }.map { it.groupId }.toSet(),
                pullsActivity = events.isNotEmpty(),
                pullsComments = Reach.COMMENTS in reaches,
                pullsAttachments = Reach.ATTACHMENTS in reaches,
                pullsOwnProfile = userId in profileIds,
                pullsMemberPhotos = profileIds.any { it != userId },
            )
        }

        // Why: every kind says what it reaches, so a kind added to ActivityKind has to say it too.
        private val ActivityKind?.reach: Reach
            get() = when (this) {
                ActivityKind.BILL_CREATED,
                ActivityKind.BILL_EDITED,
                ActivityKind.BILL_DELETED,
                ActivityKind.PAYMENT_RECORDED,
                ActivityKind.PAYMENT_UNDONE,
                ActivityKind.GROUP_CREATED,
                ActivityKind.GROUP_EDITED,
                ActivityKind.INVITE_LINK_CHANGED,
                ActivityKind.MEMBER_RENAMED,
                -> Reach.GROUP
                ActivityKind.COMMENT_ADDED -> Reach.COMMENTS
                ActivityKind.ATTACHMENT_ADDED, ActivityKind.ATTACHMENT_REMOVED -> Reach.ATTACHMENTS
                // Why: a charge and a bill's share link live only in Atividade (the link is read online, never stored).
                ActivityKind.CHARGE_SENT, ActivityKind.BILL_LINK_CHANGED -> Reach.ACTIVITY
                // Why: someone's account in or out of a group changes who is in it, their keys and photos, or the
                // group itself on this device; a guest merged into an account moves bills and drops a member.
                ActivityKind.GROUP_DELETED,
                ActivityKind.MEMBER_ADDED,
                ActivityKind.MEMBER_JOINED,
                ActivityKind.MEMBER_LEFT,
                ActivityKind.MEMBER_REMOVED,
                ActivityKind.MEMBER_RELEASED,
                null,
                -> Reach.EVERYTHING
            }
    }

    private enum class Reach {
        ACTIVITY,
        GROUP,
        COMMENTS,
        ATTACHMENTS,
        EVERYTHING,
    }
}
