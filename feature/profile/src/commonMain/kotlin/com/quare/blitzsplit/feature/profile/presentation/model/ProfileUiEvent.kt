package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ProfileUiEvent : UiEvent {
    data object OnBackClicked : ProfileUiEvent

    data object OnEditClicked : ProfileUiEvent

    data object OnVisibilityInfoClicked : ProfileUiEvent

    data object OnDialogDismissed : ProfileUiEvent

    data object OnConnectClicked : ProfileUiEvent

    data class OnProviderClicked(
        val provider: AccountProvider,
    ) : ProfileUiEvent

    data class OnIdTokenReceived(
        val provider: AccountProvider,
        val idToken: String,
        val rawNonce: String?,
        // Why: only Apple's native sheet gives it, and only on the first sign in.
        val fullName: String?,
    ) : ProfileUiEvent

    data object OnNativeSignInFailed : ProfileUiEvent

    data object OnNativeSignInCancelled : ProfileUiEvent

    data object OnRecoveryCodeClicked : ProfileUiEvent

    data object OnRetrySyncClicked : ProfileUiEvent

    data class OnThemeSelected(
        val theme: ThemePreference,
    ) : ProfileUiEvent

    data object OnPrivacyPolicyClicked : ProfileUiEvent

    data object OnTermsClicked : ProfileUiEvent

    data object OnSignOutClicked : ProfileUiEvent

    data object OnSignOutConfirmed : ProfileUiEvent

    data object OnSignOutWhenSyncedClicked : ProfileUiEvent

    data object OnCancelScheduledSignOutClicked : ProfileUiEvent

    data class OnCodeSavedChanged(
        val isChecked: Boolean,
    ) : ProfileUiEvent

    data object OnCopyCodeClicked : ProfileUiEvent

    // Why: "Conectar Google ou Apple em vez disso" in the erase dialog, which opens the account row's buttons.
    data object OnConnectInsteadClicked : ProfileUiEvent

    data object OnEraseConfirmed : ProfileUiEvent
}
