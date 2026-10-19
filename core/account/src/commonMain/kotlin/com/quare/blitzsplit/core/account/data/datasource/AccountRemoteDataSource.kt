package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.account.domain.model.RecoveryCredentials
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

interface AccountRemoteDataSource {
    // Why: emits only settled states, a user or null after a sign out, never the refresh attempts.
    fun observeSessionUser(): Flow<SessionUser?>

    suspend fun getCurrentUserOrNull(): SessionUser?

    suspend fun signInAnonymously(): SessionUser

    suspend fun linkIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    )

    suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    )

    suspend fun linkWithBrowser(provider: AccountProvider)

    suspend fun signInWithBrowser(provider: AccountProvider)

    // Why: null when the URL isn't an OAuth callback, such as a web page loaded without one.
    fun readBrowserCallback(url: String): BrowserCallback?

    suspend fun signInWithPassword(credentials: RecoveryCredentials)

    suspend fun fetchRecoveryCredentials(code: String): RecoveryCredentials

    suspend fun createRecoveryCode(): String
}
