package com.quare.blitzsplit.core.account.testing

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

// In-memory AccountRepository: public state a test can set and read, every call recorded, and
// public vars to make a call fail.
class FakeAccountRepository(
    initialStatus: AccountStatus = AccountStatus.LocalOnly,
) : AccountRepository {
    val status = MutableStateFlow(initialStatus)
    val recoveryCode = MutableStateFlow<String?>(null)
    val recoveryCodePrompt = MutableStateFlow(RecoveryCodePrompt.NOT_DUE)
    var sessionUser: SessionUser? = null

    val idTokenConnections = mutableListOf<Pair<AccountProvider, String>>()
    val idTokenFullNames = mutableListOf<String?>()
    val browserConnections = mutableListOf<AccountProvider>()
    val browserCallbacks = mutableListOf<String>()
    val restoredCodes = mutableListOf<String>()
    var registrationCount = 0
    var recoveryCodeCreations = 0

    var connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED)
    var registerError: Throwable? = null
    var restoreError: Throwable? = null
    var restoredStatus: AccountStatus = AccountStatus.Anonymous(RESTORED_USER_ID)

    // Why: like Room, a new query reads the status as it is, while an open one sees a write only once
    // requeryStatus() runs, so a test can deliver a status late.
    var defersStatusUpdates = false
    private val requeriedStatus = MutableStateFlow(initialStatus)
    var createRecoveryCodeResult: Result<String> = Result.success("K7QM4XPA9TRE")

    override fun observeStatus(): Flow<AccountStatus> = if (defersStatusUpdates) {
        flow {
            emit(status.value)
            emitAll(requeriedStatus.drop(1))
        }
    } else {
        status
    }

    fun requeryStatus() {
        requeriedStatus.value = status.value
    }

    override fun observeRecoveryCode(): Flow<String?> = recoveryCode

    override fun observeRecoveryCodePrompt(): Flow<RecoveryCodePrompt> = recoveryCodePrompt

    override suspend fun mirrorSession() {
        status.collect { }
    }

    override suspend fun getCurrentUserOrNull(): SessionUser? = sessionUser

    override suspend fun ensureRegistered(): Result<String> {
        registrationCount++
        registerError?.let { return Result.failure(it) }
        val current = status.value
        return when (current) {
            is AccountStatus.Anonymous -> Result.success(current.userId)

            is AccountStatus.Connected -> Result.success(current.userId)

            AccountStatus.LocalOnly -> {
                status.value = AccountStatus.Anonymous(ANONYMOUS_USER_ID)
                Result.success(ANONYMOUS_USER_ID)
            }
        }
    }

    override suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
    ): Result<ConnectResult> {
        idTokenConnections += provider to idToken
        idTokenFullNames += fullName
        connectResult.onSuccess { status.value = AccountStatus.Connected(CONNECTED_USER_ID, provider) }
        return connectResult
    }

    override suspend fun signInWithBrowser(provider: AccountProvider): Result<Unit> {
        browserConnections += provider
        return connectResult.map { }
    }

    override suspend fun handleBrowserCallback(url: String): Result<Unit> {
        browserCallbacks += url
        return Result.success(Unit)
    }

    override suspend fun restoreWithRecoveryCode(code: String): Result<Unit> {
        restoredCodes += code
        restoreError?.let { return Result.failure(it) }
        status.value = restoredStatus
        return Result.success(Unit)
    }

    override suspend fun createRecoveryCode(): Result<String> {
        recoveryCodeCreations++
        createRecoveryCodeResult.onSuccess { code ->
            recoveryCode.value = code
            recoveryCodePrompt.value = RecoveryCodePrompt.DUE
        }
        return createRecoveryCodeResult
    }

    override suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt) {
        recoveryCodePrompt.value = prompt
    }

    companion object {
        const val ANONYMOUS_USER_ID: String = "anonymous-user"
        const val CONNECTED_USER_ID: String = "connected-user"
        const val RESTORED_USER_ID: String = "restored-user"
    }
}
