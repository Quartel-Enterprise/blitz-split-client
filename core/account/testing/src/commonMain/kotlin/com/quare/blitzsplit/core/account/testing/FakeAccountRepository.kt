package com.quare.blitzsplit.core.account.testing

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory AccountRepository: public state a test can set and read, every call recorded, and
// public vars to make a call fail.
class FakeAccountRepository(
    initialStatus: AccountStatus = AccountStatus.LocalOnly,
) : AccountRepository {
    // Why: one flow, like the single Room row, so a write never shows a half-updated snapshot.
    val snapshot = MutableStateFlow(
        AccountSnapshot(
            status = initialStatus,
            recoveryCode = null,
            recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
            email = null,
            signOutWhenSynced = false,
        ),
    )

    var status: AccountStatus
        get() = snapshot.value.status
        set(value) {
            snapshot.update { it.copy(status = value) }
        }

    var recoveryCode: String?
        get() = snapshot.value.recoveryCode
        set(value) {
            snapshot.update { it.copy(recoveryCode = value) }
        }

    var recoveryCodePrompt: RecoveryCodePrompt
        get() = snapshot.value.recoveryCodePrompt
        set(value) {
            snapshot.update { it.copy(recoveryCodePrompt = value) }
        }

    var email: String?
        get() = snapshot.value.email
        set(value) {
            snapshot.update { it.copy(email = value) }
        }

    var signOutWhenSynced: Boolean
        get() = snapshot.value.signOutWhenSynced
        set(value) {
            snapshot.update { it.copy(signOutWhenSynced = value) }
        }
    var sessionUser: SessionUser? = null
    val abandonedBrowserSignIns = MutableSharedFlow<AbandonedBrowserSignIn>()
    val returningBrowserSignIn = MutableStateFlow<AccountProvider?>(null)

    val idTokenConnections = mutableListOf<Pair<AccountProvider, String>>()
    val idTokenFullNames = mutableListOf<String?>()
    val browserConnections = mutableListOf<AccountProvider>()
    val browserCallbacks = mutableListOf<String>()
    val restoredCodes = mutableListOf<String>()
    var registrationCount = 0
    var recoveryCodeCreations = 0
    var appPauseCount = 0
    var appResumeCount = 0
    var signOutCount = 0

    var connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED)
    var registerError: Throwable? = null
    var restoreError: Throwable? = null
    var signOutError: Throwable? = null
    var restoredStatus: AccountStatus = AccountStatus.Anonymous(RESTORED_USER_ID)

    // Why: like Room, a new query reads the status as it is, while an open one sees a write only once
    // requeryStatus() runs, so a test can deliver a status late.
    var defersStatusUpdates = false
    private val requeriedStatus = MutableStateFlow(initialStatus)
    var createRecoveryCodeResult: Result<String> = Result.success("K7QM4XPA9TRE")

    override fun observeAccount(): Flow<AccountSnapshot> = snapshot

    override fun observeStatus(): Flow<AccountStatus> = if (defersStatusUpdates) {
        flow {
            emit(status)
            emitAll(requeriedStatus.drop(1))
        }
    } else {
        snapshot.map { it.status }.distinctUntilChanged()
    }

    fun requeryStatus() {
        requeriedStatus.value = status
    }

    override suspend fun mirrorSession() {
        snapshot.collect { }
    }

    override suspend fun getCurrentUserOrNull(): SessionUser? = sessionUser

    override suspend fun ensureRegistered(): Result<String> {
        registrationCount++
        registerError?.let { return Result.failure(it) }
        val current = status
        return when (current) {
            is AccountStatus.Anonymous -> Result.success(current.userId)

            is AccountStatus.Connected -> Result.success(current.userId)

            AccountStatus.LocalOnly -> {
                status = AccountStatus.Anonymous(ANONYMOUS_USER_ID)
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
        connectResult.onSuccess { status = AccountStatus.Connected(CONNECTED_USER_ID, provider) }
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

    override fun handleAppPaused() {
        appPauseCount++
    }

    override fun handleAppResumed() {
        appResumeCount++
    }

    override fun observeAbandonedBrowserSignIn(): Flow<AbandonedBrowserSignIn> = abandonedBrowserSignIns

    override fun observeReturningBrowserSignIn(): Flow<AccountProvider?> = returningBrowserSignIn

    override suspend fun restoreWithRecoveryCode(code: String): Result<Unit> {
        restoredCodes += code
        restoreError?.let { return Result.failure(it) }
        status = restoredStatus
        return Result.success(Unit)
    }

    override suspend fun createRecoveryCode(): Result<String> {
        recoveryCodeCreations++
        createRecoveryCodeResult.onSuccess { code ->
            snapshot.update { it.copy(recoveryCode = code, recoveryCodePrompt = RecoveryCodePrompt.DUE) }
        }
        return createRecoveryCodeResult
    }

    override suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt) {
        recoveryCodePrompt = prompt
    }

    override suspend fun updateSignOutWhenSynced(isScheduled: Boolean) {
        signOutWhenSynced = isScheduled
    }

    // Why: like the real one, back to the empty row of a fresh install.
    override suspend fun signOut() {
        signOutCount++
        signOutError?.let { throw it }
        sessionUser = null
        snapshot.value = AccountSnapshot(
            status = AccountStatus.LocalOnly,
            recoveryCode = null,
            recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
            email = null,
            signOutWhenSynced = false,
        )
    }

    companion object {
        const val ANONYMOUS_USER_ID: String = "anonymous-user"
        const val CONNECTED_USER_ID: String = "connected-user"
        const val RESTORED_USER_ID: String = "restored-user"
    }
}
