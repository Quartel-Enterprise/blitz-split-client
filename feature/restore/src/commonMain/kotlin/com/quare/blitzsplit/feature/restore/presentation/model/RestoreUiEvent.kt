package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface RestoreUiEvent : UiEvent {
    data class OnCodeChanged(
        val code: String,
    ) : RestoreUiEvent

    data object OnRestoreClicked : RestoreUiEvent

    data object OnScanQrClicked : RestoreUiEvent

    data class OnQrScanned(
        val content: String,
    ) : RestoreUiEvent

    data class OnProviderClicked(
        val provider: AccountProvider,
    ) : RestoreUiEvent

    data class OnIdTokenReceived(
        val provider: AccountProvider,
        val idToken: String,
        val rawNonce: String?,
        // Why: only Apple's native sheet gives it, and only on the first sign in.
        val fullName: String?,
        // Why: Apple's native sheet only; the server trades it for the token it revokes when the account goes.
        val authorizationCode: String?,
    ) : RestoreUiEvent

    data object OnNativeSignInFailed : RestoreUiEvent

    data object OnNativeSignInCancelled : RestoreUiEvent

    data object OnStartFreshClicked : RestoreUiEvent

    data object OnBackClicked : RestoreUiEvent

    data object OnErrorDismissed : RestoreUiEvent
}
