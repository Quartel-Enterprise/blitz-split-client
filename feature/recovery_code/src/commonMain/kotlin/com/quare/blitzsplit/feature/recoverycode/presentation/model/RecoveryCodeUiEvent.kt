package com.quare.blitzsplit.feature.recoverycode.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface RecoveryCodeUiEvent : UiEvent {
    data object OnCopyClicked : RecoveryCodeUiEvent

    data object OnSaveToPasswordManagerClicked : RecoveryCodeUiEvent

    data object OnConnectInsteadClicked : RecoveryCodeUiEvent

    data class OnProviderClicked(
        val provider: AccountProvider,
    ) : RecoveryCodeUiEvent

    data class OnIdTokenReceived(
        val provider: AccountProvider,
        val idToken: String,
        val rawNonce: String?,
        // Why: only Apple's native sheet gives it, and only on the first sign in.
        val fullName: String?,
        // Why: Apple's native sheet only; the server trades it for the token it revokes when the account goes.
        val authorizationCode: String?,
    ) : RecoveryCodeUiEvent

    data object OnNativeSignInFailed : RecoveryCodeUiEvent

    data object OnNativeSignInCancelled : RecoveryCodeUiEvent

    data object OnRemindLaterClicked : RecoveryCodeUiEvent

    data object OnSavedClicked : RecoveryCodeUiEvent
}
