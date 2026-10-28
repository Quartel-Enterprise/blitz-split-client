package com.quare.blitzsplit.core.sync.signin

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class ProviderSignInImpl(
    private val nativeSignIn: NativeSignIn,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val completeSignIn: CompleteSignIn,
    private val observeAccountStatus: ObserveAccountStatus,
    private val observeReturningBrowserSignIn: ObserveReturningBrowserSignIn,
    private val observeAbandonedBrowserSignIn: ObserveAbandonedBrowserSignIn,
) : ProviderSignIn {
    /*
     * Why: the status observer leaves alone a sign in the screen completes itself, both while it runs
     * and for the Connected it produced, which Room may deliver once it has finished.
     */
    private var isSigningInDirectly = false
    private var directlyCompletedStatus: AccountStatus? = null
    private var returningProvider: AccountProvider? = null

    override suspend fun signIn(
        provider: AccountProvider,
        launchNativeSignIn: suspend (AccountProvider) -> Unit,
    ): Result<Unit> {
        if (provider !in nativeSignIn.providers) return connectWithBrowser(provider)
        launchNativeSignIn(provider)
        return Result.success(Unit)
    }

    override suspend fun onIdTokenReceived(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
    ): SignInStep = signInDirectly {
        connectWithIdToken(
            provider = provider,
            idToken = idToken,
            rawNonce = rawNonce,
            fullName = fullName,
        )
    }.getOrElse { SignInStep.Ended(hasFailed = true) }

    override suspend fun signInDirectly(signIn: suspend () -> Result<*>): Result<SignInStep> {
        isSigningInDirectly = true
        val step = signIn().map {
            directlyCompletedStatus = observeAccountStatus().first()
            completeSignIn().toSignInStep()
        }
        isSigningInDirectly = false
        return step
    }

    override fun observeBrowserSignIn(
        isShown: () -> Boolean,
        onStep: (SignInStep) -> Unit,
    ): Flow<Unit> = merge(
        observeConnectedStatus(isShown, onStep),
        observeAbandonedBrowserSignIn().map { abandoned ->
            // Why: as with a cancelled native sheet, closing the browser isn't an error, while a failed callback is.
            onStep(SignInStep.Ended(hasFailed = abandoned == AbandonedBrowserSignIn.FAILED))
        },
        observeReturningSignIn(isShown, onStep),
    )

    /*
     * Why: a browser sign in finishes when the OAuth callback comes back, after the screen's ViewModel was
     * recreated on the web (the page reloads), so a Google or Apple session seen while the screen is on top
     * completes it, with a button busy for the wait: the clicked one, or after a reload the session's, which
     * is its first identity. An ID token sign in completes on its own.
     */
    private fun observeConnectedStatus(
        isShown: () -> Boolean,
        onStep: (SignInStep) -> Unit,
    ): Flow<Unit> = observeAccountStatus()
        .distinctUntilChanged()
        .filterIsInstance<AccountStatus.Connected>()
        .filter { status -> !isSigningInDirectly && status != directlyCompletedStatus && isShown() }
        .map { status ->
            onStep(SignInStep.Started(status.provider))
            onStep(completeSignIn().toSignInStep())
        }

    /*
     * Why: on the web the callback reloads the page, so the sign in it brings back shows as under way
     * from the first frame, and as failed if it ends without the account connecting.
     */
    private fun observeReturningSignIn(
        isShown: () -> Boolean,
        onStep: (SignInStep) -> Unit,
    ): Flow<Unit> = observeReturningBrowserSignIn()
        .filter { isShown() }
        .map { provider ->
            if (provider != null) {
                returningProvider = provider
                onStep(SignInStep.Started(provider))
            } else if (returningProvider != null) {
                returningProvider = null
                val isConnected = observeAccountStatus().first() is AccountStatus.Connected
                if (!isConnected) onStep(SignInStep.Ended(hasFailed = true))
            }
        }

    private fun Result<SignInOutcome>.toSignInStep(): SignInStep = fold(
        onSuccess = SignInStep::SignedIn,
        onFailure = { SignInStep.Ended(hasFailed = true) },
    )
}
