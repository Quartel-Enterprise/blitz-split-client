package com.quare.blitzsplit.core.sync.signin

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome

// Why: what a Google or Apple sign in tells the screen, which picks its own error and its next route.
sealed interface SignInStep {
    // Why: the spinner goes on this provider, unless the screen already shows it on the one clicked.
    data class Started(
        val provider: AccountProvider,
    ) : SignInStep

    data class SignedIn(
        val outcome: SignInOutcome,
    ) : SignInStep

    data class Ended(
        val hasFailed: Boolean,
    ) : SignInStep
}
