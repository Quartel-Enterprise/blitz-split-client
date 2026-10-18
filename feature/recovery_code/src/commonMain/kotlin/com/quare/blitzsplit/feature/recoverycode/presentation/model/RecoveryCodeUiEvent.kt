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
    ) : RecoveryCodeUiEvent

    data object OnNativeSignInFailed : RecoveryCodeUiEvent

    data object OnNativeSignInCancelled : RecoveryCodeUiEvent

    data object OnRemindLaterClicked : RecoveryCodeUiEvent

    data object OnSavedClicked : RecoveryCodeUiEvent
}
