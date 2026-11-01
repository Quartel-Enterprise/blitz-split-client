package com.quare.blitzsplit.core.sync.signin

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

/*
 * Why: every screen that signs in with Google or Apple (the welcome, the restore) shows it the same way: the spinner
 * on a provider while it runs, then the groups when the account brought a profile back, or the profile setup. The
 * screen keeps the spinner and its own error in its state, through showConnecting, and guards the click itself.
 */
class ProviderSignInController(
    private val providerSignIn: ProviderSignIn,
    private val navigator: Navigator,
    private val scope: CoroutineScope,
    private val connectingProvider: () -> AccountProvider?,
    private val showConnecting: (provider: AccountProvider?, hasFailed: Boolean) -> Unit,
    private val launchNativeSignIn: suspend (AccountProvider) -> Unit,
) {
    fun observeBrowserSignIn(isShown: () -> Boolean) {
        providerSignIn
            .observeBrowserSignIn(
                isShown = isShown,
                onStep = ::handle,
            ).launchIn(scope)
    }

    fun signIn(provider: AccountProvider) {
        showConnecting(provider, false)
        scope.launch {
            providerSignIn
                .signIn(provider, launchNativeSignIn)
                .onFailure { showConnecting(null, true) }
        }
    }

    fun onIdTokenReceived(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
        authorizationCode: String?,
    ) {
        scope.launch {
            val step = providerSignIn.onIdTokenReceived(
                provider = provider,
                idToken = idToken,
                rawNonce = rawNonce,
                fullName = fullName,
                authorizationCode = authorizationCode,
            )
            handle(step)
        }
    }

    fun onNativeSignInFailed() {
        showConnecting(null, true)
    }

    fun onNativeSignInCancelled() {
        showConnecting(null, false)
    }

    // Why: a sign in the screen completes itself (the recovery code); fails with signIn's own error.
    suspend fun signInDirectly(signIn: suspend () -> Result<*>): Result<Unit> = providerSignIn
        .signInDirectly(signIn)
        .map(::handle)

    private fun handle(step: SignInStep) {
        when (step) {
            // Why: the spinner stays on the provider clicked, when the screen already shows one.
            is SignInStep.Started -> showConnecting(connectingProvider() ?: step.provider, false)

            is SignInStep.SignedIn -> {
                showConnecting(null, false)
                when (step.outcome) {
                    SignInOutcome.PROFILE_RESTORED -> navigator.replaceAll(Route.Home)
                    SignInOutcome.NEEDS_PROFILE -> navigator.navigate(Route.ProfileSetup)
                }
            }

            is SignInStep.Ended -> showConnecting(null, step.hasFailed)
        }
    }
}
