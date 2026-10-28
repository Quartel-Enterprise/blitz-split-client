package com.quare.blitzsplit.core.sync.connect

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/*
 * Why: every screen that offers Google or Apple to a profile without them (the recovery code, the profile)
 * walks the same link, so a fix to the native or browser path reaches all of them. Each screen keeps only its
 * own guards and its own sign that the link finished, and launches these calls in its own scope.
 */
interface ProviderConnector {
    val connection: StateFlow<ProviderConnection>

    fun expand()

    suspend fun connect(
        provider: AccountProvider,
        launchNativeSignIn: suspend (AccountProvider) -> Unit,
    )

    suspend fun onIdTokenReceived(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
    )

    fun finishConnecting(hasFailed: Boolean)

    fun observeBrowserSignIn(hasConnected: (AccountSnapshot) -> Boolean): Flow<Unit>
}
