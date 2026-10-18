package com.quare.blitzsplit.core.account.domain.repository

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeStatus(): Flow<AccountStatus>

    fun observeRecoveryCode(): Flow<String?>

    fun observeRecoveryCodePrompt(): Flow<RecoveryCodePrompt>

    suspend fun mirrorSession()

    suspend fun getCurrentUserOrNull(): SessionUser?

    suspend fun ensureRegistered(): Result<String>

    suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ): Result<ConnectResult>

    suspend fun signInWithBrowser(provider: AccountProvider): Result<Unit>

    suspend fun restoreWithRecoveryCode(code: String): Result<Unit>

    suspend fun createRecoveryCode(): Result<String>

    suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt)
}
