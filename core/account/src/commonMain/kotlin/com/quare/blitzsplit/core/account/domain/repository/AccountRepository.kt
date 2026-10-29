package com.quare.blitzsplit.core.account.domain.repository

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccount(): Flow<AccountSnapshot>

    fun observeStatus(): Flow<AccountStatus>

    suspend fun mirrorSession()

    // Why: never returns, like mirrorSession: sends the server each Apple token a browser sign in brings.
    suspend fun keepAppleTokens()

    suspend fun getCurrentUserOrNull(): SessionUser?

    suspend fun ensureRegistered(): Result<String>

    suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
        authorizationCode: String?,
    ): Result<ConnectResult>

    suspend fun signInWithBrowser(
        provider: AccountProvider,
        isFromProfile: Boolean,
    ): Result<Unit>

    suspend fun handleBrowserCallback(url: String): Result<Unit>

    fun handleAppPaused()

    fun handleAppResumed()

    fun observeAbandonedBrowserSignIn(): Flow<AbandonedBrowserSignIn>

    fun observeReturningBrowserSignIn(): Flow<AccountProvider?>

    // Why: a sign in left from the profile whose callback started the app again, which lands on the groups.
    fun observeBrowserSignInReturningToProfile(): Flow<AccountProvider?>

    suspend fun restoreWithRecoveryCode(code: String): Result<Unit>

    suspend fun createRecoveryCode(): Result<String>

    suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt)

    suspend fun updateSignOutWhenSynced(isScheduled: Boolean)

    suspend fun signOut()

    // Why: the Google or Apple account goes and a new recovery code comes, which the prompt then shows.
    suspend fun removeSocialIdentity(): Result<Unit>

    // Why: only the server's copy; the device's goes with endDeletedSession and the profile's and keys' clears.
    suspend fun deleteAccount(): Result<Unit>

    // Why: the session of a user the server no longer has ends here, with no logout call that could only fail.
    suspend fun endDeletedSession()
}
