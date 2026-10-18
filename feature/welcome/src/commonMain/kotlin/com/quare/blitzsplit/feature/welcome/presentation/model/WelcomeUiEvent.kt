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
    ) : WelcomeUiEvent

    data object OnNativeSignInFailed : WelcomeUiEvent

    data object OnNativeSignInCancelled : WelcomeUiEvent

    data object OnUseWithoutAccountClicked : WelcomeUiEvent

    data object OnUsedOnAnotherDeviceClicked : WelcomeUiEvent

    data object OnErrorDismissed : WelcomeUiEvent
}
