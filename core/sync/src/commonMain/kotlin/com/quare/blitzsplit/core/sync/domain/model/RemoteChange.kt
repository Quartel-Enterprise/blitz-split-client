package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind

/*
 * Why: what real time tells this device (beta feedback 13) of another device's change. Only what to pull, never the
 * rows: they come down through the tables as the sync brings them, so Room stays the one source of truth.
 */
sealed interface RemoteChange {
    // Why: the channel joined, the first time or again after the socket dropped, so whatever happened meanwhile is unheard.
    data object Joined : RemoteChange

    // Why: an event of Atividade, which the server records for everything done in a group; null for a kind a newer server records.
    data class GroupEvent(
        val seq: Long,
        val groupId: String,
        val kind: ActivityKind?,
    ) : RemoteChange

    // Why: a profile saved: this user's own on another device, or the photo of someone they share a group with.
    data class ProfileChanged(
        val userId: String,
    ) : RemoteChange
}
