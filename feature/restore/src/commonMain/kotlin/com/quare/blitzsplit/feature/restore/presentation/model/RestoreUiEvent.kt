package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.SignInAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "restore"

sealed interface RestoreUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnCodeChanged -> EventAnalytics.NotTracked

            // Why: the same button is "Tentar de novo" while offline, which submits nothing.
            OnRestoreClicked -> EventAnalytics.Manual(setOf("restore_code_submitted", "retry_clicked"))

            OnScanQrClicked -> EventAnalytics.Automatic("restore_scan_qr_clicked")

            // Why: the camera handing back what it read, which OnRestoreClicked's path then checks.
            is OnQrScanned -> EventAnalytics.NotTracked

            is OnProviderClicked -> SignInAnalytics.buildProviderClicked(provider, SOURCE)

            is OnIdTokenReceived -> EventAnalytics.NotTracked

            OnNativeSignInFailed -> SignInAnalytics.buildNativeSignInFailed(SOURCE)

            OnNativeSignInCancelled -> SignInAnalytics.buildNativeSignInCancelled(SOURCE)

            OnStartFreshClicked -> EventAnalytics.Automatic("restore_start_fresh_clicked")

            /*
             * Why: each platform's own back (a gesture, the NavigationStack, a swipe down) reaches the back stack apart, so a
             * back counts alike only as the screen_view of where it went.
             */
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: no UI sends it today: an error goes away with the screen's next action.
            OnErrorDismissed -> EventAnalytics.NotTracked
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
