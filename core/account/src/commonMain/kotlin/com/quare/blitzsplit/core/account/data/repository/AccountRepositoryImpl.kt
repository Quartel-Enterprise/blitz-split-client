package com.quare.blitzsplit.core.account.data.repository

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
import com.quare.blitzsplit.core.account.data.datasource.BrowserCallback
import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.IdentityAlreadyLinkedException
import com.quare.blitzsplit.core.account.domain.model.InvalidRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.model.SocialAccountHasNoRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class AccountRepositoryImpl(
    private val local: AccountLocalDataSource,
    private val remote: AccountRemoteDataSource,
) : AccountRepository {
    // Why: in memory, since a browser trip doesn't outlive the process that opened it.
    private val browserTrip = AtomicReference(BrowserTrip.NONE)
    private val abandonedBrowserSignIns = MutableSharedFlow<AbandonedBrowserSignIn>(extraBufferCapacity = 1)

    /*
     * Why: from a browser callback that opened the app until the session it brings is mirrored, so the
     * screens show that sign in as under way across the page reload of the web. A callback the mirror
     * already settled doesn't show it again once its provider is read.
     */
    private val returningSignIn =
        MutableStateFlow(ReturningSignIn(provider = null, isSettled = true, returnsToProfile = false))

    override fun observeAccount(): Flow<AccountSnapshot> = local
        .observe()
        .map(::toSnapshot)
        .distinctUntilChanged()

    override fun observeStatus(): Flow<AccountStatus> = observeAccount()
        .map { it.status }
        .distinctUntilChanged()

    override suspend fun mirrorSession() {
        remote.observeSessionUser().collect { user ->
            local.update { account -> account.toMirrorOf(user) }
            returningSignIn.value = ReturningSignIn(provider = null, isSettled = true, returnsToProfile = false)
        }
    }

    override suspend fun getCurrentUserOrNull(): SessionUser? = remote.getCurrentUserOrNull()

    override suspend fun ensureRegistered(): Result<String> = suspendRunCatching {
        val user = remote.getCurrentUserOrNull() ?: remote.signInAnonymously()
        local.update { account -> account.toMirrorOf(user) }
        user.userId
    }

    override suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
    ): Result<ConnectResult> = suspendRunCatching {
        val current = remote.getCurrentUserOrNull()
        val result = when {
            current == null -> {
                remote.signInWithIdToken(provider = provider, idToken = idToken, rawNonce = rawNonce)
                ConnectResult.SIGNED_IN
            }

            current.socialProvider != null -> {
                remote.signInWithIdToken(provider = provider, idToken = idToken, rawNonce = rawNonce)
                ConnectResult.SWITCHED
            }

            else -> signInLinkingFirst(provider = provider, idToken = idToken, rawNonce = rawNonce)
        }
        remote.getCurrentUserOrNull()?.let { user ->
            local.update { account -> account.toMirrorOf(user) }
            val givenName = fullName?.takeIf(String::isNotBlank)
            if (user.displayName == null && givenName != null) keepNameGivenOnce(givenName)
        }
        result
    }

    override suspend fun signInWithBrowser(
        provider: AccountProvider,
        isFromProfile: Boolean,
    ): Result<Unit> = suspendRunCatching {
        val current = remote.getCurrentUserOrNull()
        if (current != null && current.socialProvider == null) {
            linkWithBrowser(provider = provider, isFromProfile = isFromProfile)
        } else {
            local.update { account ->
                account.copy(
                    pendingLinkProvider = null,
                    browserSignInProvider = provider.code,
                    browserSignInFromProfile = isFromProfile,
                )
            }
            openBrowser { remote.signInWithBrowser(provider) }
        }
    }

    /*
     * Why: Supabase drops a callback's error while a session is signed in, as the anonymous one is
     * during a link, so the app reads the URL itself. Like the ID token path, an identity that
     * already belongs to another user signs in to that user, here with a second browser trip.
     */
    override suspend fun handleBrowserCallback(url: String): Result<Unit> {
        val callback = remote.readBrowserCallback(url) ?: return Result.success(Unit)
        // Why: before any suspension, so the app resuming right after the callback finds the trip closed.
        val wasBrowserOpen = browserTrip.exchange(BrowserTrip.NONE) != BrowserTrip.NONE
        return suspendRunCatching { finishBrowserTrip(callback = callback, wasBrowserOpen = wasBrowserOpen) }
            .onFailure {
                // Why: no session will come to end the screens' wait, unless Supabase signed in or a second trip is open.
                val endsNoSession = callback != BrowserCallback.SIGNED_IN && browserTrip.load() == BrowserTrip.NONE
                if (wasBrowserOpen && endsNoSession) {
                    abandonedBrowserSignIns.tryEmit(AbandonedBrowserSignIn.FAILED)
                }
            }
    }

    // Why: the app leaves for the browser, so only a trip that saw it leave can have been abandoned.
    override fun handleAppPaused() {
        browserTrip.compareAndSet(BrowserTrip.OPEN, BrowserTrip.LEFT)
    }

    /*
     * Why: the OAuth callback reaches the app before it resumes, so a browser still open once the app
     * is back was closed without finishing, and no callback will end its sign in.
     */
    override fun handleAppResumed() {
        if (browserTrip.compareAndSet(BrowserTrip.LEFT, BrowserTrip.NONE)) {
            abandonedBrowserSignIns.tryEmit(AbandonedBrowserSignIn.CANCELLED)
        }
    }

    override fun observeAbandonedBrowserSignIn(): Flow<AbandonedBrowserSignIn> = abandonedBrowserSignIns

    override fun observeReturningBrowserSignIn(): Flow<AccountProvider?> = returningSignIn
        .map { it.provider }
        .distinctUntilChanged()

    override fun observeBrowserSignInReturningToProfile(): Flow<AccountProvider?> = returningSignIn
        .map { returning -> returning.provider?.takeIf { returning.returnsToProfile } }
        .distinctUntilChanged()

    override suspend fun restoreWithRecoveryCode(code: String): Result<Unit> = suspendRunCatching {
        val normalized = RecoveryCodeFormat.normalize(code) ?: throw InvalidRecoveryCodeException()
        val credentials = remote.fetchRecoveryCredentials(normalized)
        remote.signInWithPassword(credentials)
        val user = requireNotNull(remote.getCurrentUserOrNull())
        local.update { account ->
            account.toMirrorOf(user).copy(
                recoveryCode = normalized,
                recoveryCodePrompt = RecoveryCodePrompt.SAVED.code,
            )
        }
    }

    override suspend fun createRecoveryCode(): Result<String> = suspendRunCatching {
        try {
            val code = remote.createRecoveryCode()
            local.update { account ->
                account.copy(recoveryCode = code, recoveryCodePrompt = RecoveryCodePrompt.DUE.code)
            }
            code
        } catch (exception: SocialAccountHasNoRecoveryCodeException) {
            local.update { account -> account.copy(recoveryCodePrompt = RecoveryCodePrompt.SAVED.code) }
            throw exception
        }
    }

    override suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt) {
        local.update { account -> account.copy(recoveryCodePrompt = prompt.code) }
    }

    override suspend fun updateSignOutWhenSynced(isScheduled: Boolean) {
        local.update { account -> account.copy(signOutWhenSynced = isScheduled) }
    }

    // Why: the anonymous user stays on the server, where its recovery code still brings it back.
    override suspend fun signOut() {
        remote.signOut()
        local.clear()
    }

    /*
     * Why: Apple tells the name only on the first sign in and leaves it out of the ID token, so the
     * account keeps it for the profile prefill. A failure doesn't fail the sign in: the name can be typed.
     */
    private suspend fun keepNameGivenOnce(name: String) {
        suspendRunCatching { remote.saveDisplayName(name) }
    }

    private suspend fun linkWithBrowser(
        provider: AccountProvider,
        isFromProfile: Boolean,
    ) {
        local.update { account ->
            account.copy(
                pendingLinkProvider = provider.code,
                browserSignInProvider = provider.code,
                browserSignInFromProfile = isFromProfile,
            )
        }
        try {
            openBrowser { remote.linkWithBrowser(provider) }
        } catch (exception: Exception) {
            local.update { account -> account.copy(pendingLinkProvider = null) }
            throw exception
        }
    }

    private suspend fun finishBrowserTrip(
        callback: BrowserCallback,
        wasBrowserOpen: Boolean,
    ) {
        if (callback == BrowserCallback.SIGNED_IN) showReturningSignIn(isRelaunch = !wasBrowserOpen)
        val linkingProvider = local.get().pendingLinkProvider?.let(AccountProvider::fromCodeOrNull)
        val startsSecondTrip = callback == BrowserCallback.IDENTITY_ALREADY_LINKED && linkingProvider != null
        // Why: only a sign in leaves a session that ends the screens' wait, so any other callback ends it here.
        val abandoned = when (callback) {
            BrowserCallback.SIGNED_IN -> null
            BrowserCallback.CANCELLED -> AbandonedBrowserSignIn.CANCELLED
            BrowserCallback.FAILED -> AbandonedBrowserSignIn.FAILED
            BrowserCallback.IDENTITY_ALREADY_LINKED -> AbandonedBrowserSignIn.FAILED.takeUnless { startsSecondTrip }
        }
        if (wasBrowserOpen && abandoned != null) abandonedBrowserSignIns.tryEmit(abandoned)
        if (linkingProvider == null) return
        // Why: cleared once the second trip has started, so a failed start can be retried with the same URL.
        if (startsSecondTrip) openBrowser { remote.signInWithBrowser(linkingProvider) }
        local.update { account -> account.copy(pendingLinkProvider = null) }
    }

    private suspend fun openBrowser(open: suspend () -> Unit) {
        browserTrip.store(BrowserTrip.OPEN)
        try {
            open()
        } catch (exception: Exception) {
            browserTrip.store(BrowserTrip.NONE)
            throw exception
        }
    }

    // Why: reset first, so the mirror of this callback's session settles it even before the provider is read.
    private suspend fun showReturningSignIn(isRelaunch: Boolean) {
        returningSignIn.value = ReturningSignIn(provider = null, isSettled = false, returnsToProfile = false)
        val account = local.get()
        val provider = account.browserSignInProvider?.let(AccountProvider::fromCodeOrNull) ?: return
        val returnsToProfile = isRelaunch && account.browserSignInFromProfile
        returningSignIn.update { current ->
            if (current.isSettled) current else current.copy(provider = provider, returnsToProfile = returnsToProfile)
        }
    }

    private suspend fun signInLinkingFirst(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ): ConnectResult = try {
        remote.linkIdToken(provider = provider, idToken = idToken, rawNonce = rawNonce)
        ConnectResult.LINKED
    } catch (alreadyLinked: IdentityAlreadyLinkedException) {
        remote.signInWithIdToken(provider = provider, idToken = idToken, rawNonce = rawNonce)
        ConnectResult.SWITCHED
    }

    private fun toSnapshot(account: AccountEntity): AccountSnapshot = AccountSnapshot(
        status = toStatus(account),
        recoveryCode = account.recoveryCode,
        recoveryCodePrompt = RecoveryCodePrompt
            .fromCodeOrNull(account.recoveryCodePrompt)
            ?: RecoveryCodePrompt.NOT_DUE,
        email = account.email,
        signOutWhenSynced = account.signOutWhenSynced,
    )

    private fun toStatus(account: AccountEntity): AccountStatus {
        val userId = account.userId ?: return AccountStatus.LocalOnly
        val provider = account.provider?.let(AccountProvider::fromCodeOrNull)
            ?: return AccountStatus.Anonymous(userId)
        return AccountStatus.Connected(userId = userId, provider = provider)
    }

    /*
     * Why: a Google or Apple account replaces the recovery code, so its prompt is settled for good. A
     * session that ends on its own drops a scheduled sign out, which was for that account: the changes
     * it waited on stay on the device.
     */
    private fun AccountEntity.toMirrorOf(user: SessionUser?): AccountEntity = when {
        user == null -> copy(
            userId = null,
            provider = null,
            pendingLinkProvider = null,
            email = null,
            signOutWhenSynced = false,
        )

        user.socialProvider != null -> copy(
            userId = user.userId,
            provider = user.socialProvider.code,
            recoveryCodePrompt = RecoveryCodePrompt.SAVED.code,
            email = user.email,
        )

        else -> copy(userId = user.userId, provider = null, email = null)
    }

    private enum class BrowserTrip {
        NONE,
        OPEN,
        LEFT,
    }
}
