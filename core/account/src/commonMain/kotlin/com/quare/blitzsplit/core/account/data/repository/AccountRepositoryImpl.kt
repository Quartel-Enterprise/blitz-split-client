package com.quare.blitzsplit.core.account.data.repository

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
import com.quare.blitzsplit.core.account.data.datasource.BrowserCallback
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class AccountRepositoryImpl(
    private val local: AccountLocalDataSource,
    private val remote: AccountRemoteDataSource,
) : AccountRepository {
    /*
     * Why: from a browser callback that opened the app until the session it brings is mirrored, so the
     * screens show that sign in as under way across the page reload of the web. A callback the mirror
     * already settled doesn't show it again once its provider is read.
     */
    private val returningSignIn = MutableStateFlow(ReturningSignIn(provider = null, isSettled = true))

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
            returningSignIn.value = ReturningSignIn(provider = null, isSettled = true)
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

    override suspend fun signInWithBrowser(provider: AccountProvider): Result<Unit> = suspendRunCatching {
        val current = remote.getCurrentUserOrNull()
        if (current != null && current.socialProvider == null) {
            linkWithBrowser(provider)
        } else {
            local.update { account -> account.copy(pendingLinkProvider = null, browserSignInProvider = provider.code) }
            remote.signInWithBrowser(provider)
        }
    }

    /*
     * Why: Supabase drops a callback's error while a session is signed in, as the anonymous one is
     * during a link, so the app reads the URL itself. Like the ID token path, an identity that
     * already belongs to another user signs in to that user, here with a second browser trip.
     */
    override suspend fun handleBrowserCallback(url: String): Result<Unit> = suspendRunCatching {
        val callback = remote.readBrowserCallback(url) ?: return@suspendRunCatching
        if (callback == BrowserCallback.SIGNED_IN) showReturningSignIn()
        val linkingProvider = local.get().pendingLinkProvider?.let(AccountProvider::fromCodeOrNull)
            ?: return@suspendRunCatching
        // Why: cleared once the second trip has started, so a failed start can be retried with the same URL.
        if (callback == BrowserCallback.IDENTITY_ALREADY_LINKED) {
            remote.signInWithBrowser(linkingProvider)
        }
        local.update { account -> account.copy(pendingLinkProvider = null) }
    }

    override fun observeReturningBrowserSignIn(): Flow<AccountProvider?> = returningSignIn
        .map { it.provider }
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

    /*
     * Why: Apple tells the name only on the first sign in and leaves it out of the ID token, so the
     * account keeps it for the profile prefill. A failure doesn't fail the sign in: the name can be typed.
     */
    private suspend fun keepNameGivenOnce(name: String) {
        suspendRunCatching { remote.saveDisplayName(name) }
    }

    private suspend fun linkWithBrowser(provider: AccountProvider) {
        local.update { account ->
            account.copy(pendingLinkProvider = provider.code, browserSignInProvider = provider.code)
        }
        try {
            remote.linkWithBrowser(provider)
        } catch (exception: Exception) {
            local.update { account -> account.copy(pendingLinkProvider = null) }
            throw exception
        }
    }

    // Why: reset first, so the mirror of this callback's session settles it even before the provider is read.
    private suspend fun showReturningSignIn() {
        returningSignIn.value = ReturningSignIn(provider = null, isSettled = false)
        val provider = local.get().browserSignInProvider?.let(AccountProvider::fromCodeOrNull) ?: return
        returningSignIn.update { current -> if (current.isSettled) current else current.copy(provider = provider) }
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
    )

    private fun toStatus(account: AccountEntity): AccountStatus {
        val userId = account.userId ?: return AccountStatus.LocalOnly
        val provider = account.provider?.let(AccountProvider::fromCodeOrNull)
            ?: return AccountStatus.Anonymous(userId)
        return AccountStatus.Connected(userId = userId, provider = provider)
    }

    // Why: a Google or Apple account replaces the recovery code, so its prompt is settled for good.
    private fun AccountEntity.toMirrorOf(user: SessionUser?): AccountEntity = when {
        user == null -> copy(userId = null, provider = null, pendingLinkProvider = null)

        user.socialProvider != null -> copy(
            userId = user.userId,
            provider = user.socialProvider.code,
            recoveryCodePrompt = RecoveryCodePrompt.SAVED.code,
        )

        else -> copy(userId = user.userId, provider = null)
    }
}
