package com.quare.blitzsplit.core.account.data.repository

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val local: AccountLocalDataSource,
    private val remote: AccountRemoteDataSource,
) : AccountRepository {
    override fun observeStatus(): Flow<AccountStatus> = local
        .observe()
        .map(::toStatus)
        .distinctUntilChanged()

    override fun observeRecoveryCode(): Flow<String?> = local
        .observe()
        .map { it.recoveryCode }
        .distinctUntilChanged()

    override fun observeRecoveryCodePrompt(): Flow<RecoveryCodePrompt> = local
        .observe()
        .map { RecoveryCodePrompt.fromCodeOrNull(it.recoveryCodePrompt) ?: RecoveryCodePrompt.NOT_DUE }
        .distinctUntilChanged()

    override suspend fun mirrorSession() {
        remote.observeSessionUser().collect { user ->
            local.update { account -> account.toMirrorOf(user) }
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
        remote.getCurrentUserOrNull()?.let { user -> local.update { account -> account.toMirrorOf(user) } }
        result
    }

    override suspend fun signInWithBrowser(provider: AccountProvider): Result<Unit> = suspendRunCatching {
        val current = remote.getCurrentUserOrNull()
        if (current != null && current.socialProvider == null) {
            remote.linkWithBrowser(provider)
        } else {
            remote.signInWithBrowser(provider)
        }
    }

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

    private fun toStatus(account: AccountEntity): AccountStatus {
        val userId = account.userId ?: return AccountStatus.LocalOnly
        val provider = account.provider?.let(AccountProvider::fromCodeOrNull)
            ?: return AccountStatus.Anonymous(userId)
        return AccountStatus.Connected(userId = userId, provider = provider)
    }

    // Why: a Google or Apple account replaces the recovery code, so its prompt is settled for good.
    private fun AccountEntity.toMirrorOf(user: SessionUser?): AccountEntity = when {
        user == null -> copy(userId = null, provider = null)

        user.socialProvider != null -> copy(
            userId = user.userId,
            provider = user.socialProvider.code,
            recoveryCodePrompt = RecoveryCodePrompt.SAVED.code,
        )

        else -> copy(userId = user.userId, provider = null)
    }
}
