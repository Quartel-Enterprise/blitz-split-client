package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "restore"

sealed interface RestoreUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnCodeChanged -> EventAnalytics.NotTracked

            OnRestoreClicked -> EventAnalytics.Automatic("restore_code_submitted")

            OnScanQrClicked -> EventAnalytics.Automatic("restore_scan_qr_clicked")

            // Why: the camera handing back what it read, which OnRestoreClicked's path then checks.
            is OnQrScanned -> EventAnalytics.NotTracked

            is OnProviderClicked -> EventAnalytics.Automatic(
                "sign_in_provider_clicked",
                mapOf("provider" to provider, "source" to SOURCE),
            )

            is OnIdTokenReceived -> EventAnalytics.NotTracked

            OnNativeSignInFailed -> EventAnalytics.Automatic("native_sign_in_failed", mapOf("source" to SOURCE))

            OnNativeSignInCancelled -> EventAnalytics.Automatic("native_sign_in_cancelled", mapOf("source" to SOURCE))

            OnStartFreshClicked -> EventAnalytics.Automatic("restore_start_fresh_clicked")

            OnBackClicked -> EventAnalytics.Automatic("back_clicked", mapOf("source" to SOURCE))

            OnErrorDismissed -> EventAnalytics.Automatic("error_dismissed", mapOf("source" to SOURCE))
        }

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
