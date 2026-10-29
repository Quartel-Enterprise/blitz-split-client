package com.quare.blitzsplit.feature.welcome.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface WelcomeUiEvent : UiEvent {
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
