package com.quare.blitzsplit.core.sync.connect

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update

class ProviderConnectorImpl(
    private val nativeSignIn: NativeSignIn,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val completeSignIn: CompleteSignIn,
    private val observeAccount: ObserveAccount,
    private val observeReturningBrowserSignIn: ObserveReturningBrowserSignIn,
    private val observeAbandonedBrowserSignIn: ObserveAbandonedBrowserSignIn,
) : ProviderConnector {
    override val connection: StateFlow<ProviderConnection>
        field = MutableStateFlow(
            ProviderConnection(
                connectingProvider = null,
                hasFailed = false,
                isExpanded = false,
            ),
        )

    private var returningProvider: AccountProvider? = null

    override fun expand() {
        connection.update { it.copy(isExpanded = true) }
    }

    // Why: the screens' guards read a copy of this state, so a second tap before it lands mustn't start another link.
    override suspend fun connect(
        provider: AccountProvider,
        launchNativeSignIn: suspend (AccountProvider) -> Unit,
    ) {
        if (connection.value.connectingProvider != null) return
        connection.update {
            it.copy(
                connectingProvider = provider,
                hasFailed = false,
            )
        }
        if (provider in nativeSignIn.providers) {
            launchNativeSignIn(provider)
        } else {
            connectWithBrowser(provider).onFailure { finishConnecting(hasFailed = true) }
        }
    }

    /*
     * Why: the link stands once the identity is in, so a failed pull doesn't fail it. The recovery code and the
     * profile already have a profile, so no step asks for a new one, and every sync pushes nothing when its
     * pull fails. A switch to another user starts one right away, whose failure shows on the sync badge.
     */
    override suspend fun onIdTokenReceived(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
    ) {
        connectWithIdToken(
            provider = provider,
            idToken = idToken,
            rawNonce = rawNonce,
            fullName = fullName,
        ).onSuccess {
            completeSignIn()
            finishConnecting(hasFailed = false)
        }.onFailure { finishConnecting(hasFailed = true) }
    }

    override fun finishConnecting(hasFailed: Boolean) {
        connection.update {
            it.copy(
                connectingProvider = null,
                hasFailed = hasFailed,
            )
        }
    }

    /*
     * Why: on the web the link's callback reloads the page, so the link shows as under way meanwhile, and as
     * failed if it ends without the account the screen waits for. As with a cancelled native sheet, closing
     * the browser isn't a failure, while a failed callback is.
     */
    override fun observeBrowserSignIn(hasConnected: (AccountSnapshot) -> Boolean): Flow<Unit> = merge(
        observeAbandonedBrowserSignIn().map { abandoned ->
            finishConnecting(hasFailed = abandoned == AbandonedBrowserSignIn.FAILED)
        },
        observeReturningBrowserSignIn().map { provider -> handleReturningSignIn(provider, hasConnected) },
    )

    private suspend fun handleReturningSignIn(
        provider: AccountProvider?,
        hasConnected: (AccountSnapshot) -> Boolean,
    ) {
        if (provider != null) {
            returningProvider = provider
            connection.update {
                it.copy(
                    connectingProvider = it.connectingProvider ?: provider,
                    hasFailed = false,
                    isExpanded = true,
                )
            }
        } else if (returningProvider != null) {
            returningProvider = null
            finishConnecting(hasFailed = !hasConnected(observeAccount().first()))
        }
    }
}
