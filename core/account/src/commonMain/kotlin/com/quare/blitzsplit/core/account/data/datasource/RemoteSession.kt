package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.account.domain.model.SessionUser

/*
 * Why: the session as one read, so a refresh that settles between two reads can't make a session waiting for its
 * refresh look like none, which a sign in would then replace with another user.
 */
sealed interface RemoteSession {
    data class Active(
        val user: SessionUser,
    ) : RemoteSession

    // Why: kept on the device, but its token expired and couldn't be refreshed yet (no internet after a resume).
    data object Refreshing : RemoteSession

    data object None : RemoteSession
}
