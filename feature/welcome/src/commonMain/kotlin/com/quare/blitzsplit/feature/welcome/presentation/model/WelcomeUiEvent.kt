package com.quare.blitzsplit.feature.welcome.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.SignInAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "welcome"

sealed interface WelcomeUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnProviderClicked -> SignInAnalytics.buildProviderClicked(provider, SOURCE)

            // Why: the platform's sheet handing back the token, whose sign in the server then completes.
            is OnIdTokenReceived -> EventAnalytics.NotTracked

            OnNativeSignInFailed -> SignInAnalytics.buildNativeSignInFailed(SOURCE)

            OnNativeSignInCancelled -> SignInAnalytics.buildNativeSignInCancelled(SOURCE)

            OnUseWithoutAccountClicked -> EventAnalytics.Automatic("welcome_use_without_account_clicked")

            OnUsedOnAnotherDeviceClicked -> EventAnalytics.Automatic("welcome_restore_clicked")

            // Why: no UI sends it today: an error goes away with the screen's next action.
            OnErrorDismissed -> EventAnalytics.NotTracked
        }

    data class OnProviderClicked(
        val provider: AccountProvider,
    ) : WelcomeUiEvent

    // Why: the platform's native button (Credential Manager, Sign in with Apple) returned a token.
    data class OnIdTokenReceived(
        val provider: AccountProvider,
        val idToken: String,
        val rawNonce: String?,
        // Why: only Apple's native sheet gives it, and only on the first sign in.
        val fullName: String?,
        // Why: Apple's native sheet only; the server trades it for the token it revokes when the account goes.
        val authorizationCode: String?,
    ) : WelcomeUiEvent

    data object OnNativeSignInFailed : WelcomeUiEvent

    data object OnNativeSignInCancelled : WelcomeUiEvent

    data object OnUseWithoutAccountClicked : WelcomeUiEvent

    data object OnUsedOnAnotherDeviceClicked : WelcomeUiEvent

    data object OnErrorDismissed : WelcomeUiEvent
}
