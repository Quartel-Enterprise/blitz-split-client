package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.account.domain.model.AppleToken
import com.quare.blitzsplit.core.account.domain.model.RecoveryCredentials
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

interface AccountRemoteDataSource {
    // Why: emits only settled states, a user or null after a sign out, never the refresh attempts.
    fun observeSessionUser(): Flow<SessionUser?>

    suspend fun getCurrentUserOrNull(): SessionUser?

    // Why: Apple's refresh token of each browser sign in to a user with an Apple identity, once.
    fun observeAppleRefreshTokens(): Flow<String>

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

    suspend fun saveDisplayName(name: String)

    suspend fun linkWithBrowser(provider: AccountProvider)

    suspend fun signInWithBrowser(provider: AccountProvider)

    // Why: null when the URL isn't an OAuth callback, such as a web page loaded without one.
    fun readBrowserCallback(url: String): BrowserCallback?

    suspend fun signInWithPassword(credentials: RecoveryCredentials)

    suspend fun fetchRecoveryCredentials(code: String): RecoveryCredentials

    suspend fun createRecoveryCode(): String

    suspend fun keepAppleToken(token: AppleToken)

    // Why: returns the new recovery code; the session's user is read again, now without the identity.
    suspend fun removeSocialIdentity(): String

    suspend fun deleteAccount()

    // Why: ends the session here only, for a user the server no longer has, whose logout could only fail.
    suspend fun clearLocalSession()

    // Why: ends the session on this device even offline, when the server can't be told.
    suspend fun signOut()
}
