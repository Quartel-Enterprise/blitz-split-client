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

    suspend fun getCurrentUserOrNull(): SessionUser?

    suspend fun ensureRegistered(): Result<String>

    suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
    ): Result<ConnectResult>

    suspend fun signInWithBrowser(provider: AccountProvider): Result<Unit>

    suspend fun handleBrowserCallback(url: String): Result<Unit>

    fun handleAppPaused()

    fun handleAppResumed()

    fun observeAbandonedBrowserSignIn(): Flow<AbandonedBrowserSignIn>

    fun observeReturningBrowserSignIn(): Flow<AccountProvider?>

    // Why: only the sign ins whose callback started the app again, which lands on the start screen.
    fun observeRelaunchingBrowserSignIn(): Flow<AccountProvider?>

    suspend fun restoreWithRecoveryCode(code: String): Result<Unit>

    suspend fun createRecoveryCode(): Result<String>

    suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt)

    suspend fun updateSignOutWhenSynced(isScheduled: Boolean)

    suspend fun signOut()
}
