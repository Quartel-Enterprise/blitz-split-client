package com.quare.blitzsplit.core.groups.domain.repository

import kotlinx.coroutines.flow.Flow

/*
 * Why: Invite's "Add someone without the app" opens the group's Members with "Add person" already open, and the
 * screen it lands on may already be alive under Invite, so the ask goes through here rather than a route.
 */
interface AddPersonRequestRepository {
    // Why: the group whose "Add person" is still to open, null while none is.
    fun observeRequest(): Flow<String?>

    fun post(groupId: String)

    // Why: once the "Add person" it opened closes, not when it opens: a screen mounted twice must open it again.
    fun consume(groupId: String)
}
