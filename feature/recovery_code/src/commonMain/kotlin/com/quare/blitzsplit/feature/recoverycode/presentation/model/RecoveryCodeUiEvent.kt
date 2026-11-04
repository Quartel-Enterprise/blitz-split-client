package com.quare.blitzsplit.feature.recoverycode.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "recovery_code"

sealed interface RecoveryCodeUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnCopyClicked -> EventAnalytics.Automatic("recovery_code_copy_clicked")

            OnSaveToPasswordManagerClicked -> EventAnalytics.Automatic("recovery_code_password_manager_clicked")

            OnConnectInsteadClicked -> EventAnalytics.Automatic("recovery_code_connect_instead_clicked")

            is OnProviderClicked -> EventAnalytics.Automatic(
                "sign_in_provider_clicked",
                mapOf("provider" to provider, "source" to SOURCE),
            )

            is OnIdTokenReceived -> EventAnalytics.NotTracked

            OnNativeSignInFailed -> EventAnalytics.Automatic("native_sign_in_failed", mapOf("source" to SOURCE))

            OnNativeSignInCancelled -> EventAnalytics.Automatic("native_sign_in_cancelled", mapOf("source" to SOURCE))

            OnRemindLaterClicked -> EventAnalytics.Automatic("recovery_code_remind_later_clicked")

            OnSavedClicked -> EventAnalytics.Automatic("recovery_code_saved_clicked")
        }

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
