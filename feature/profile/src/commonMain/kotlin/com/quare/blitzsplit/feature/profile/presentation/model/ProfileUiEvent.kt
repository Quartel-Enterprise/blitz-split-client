package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.SignInAnalytics

// Why: the screen these events come from, as their source param says, for its UiEvent and its ViewModel alike.
internal const val PROFILE_SOURCE = "profile"

sealed interface ProfileUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: what the platform's sign in hands back, which the tap on its button already counted.
            is OnIdTokenReceived -> EventAnalytics.NotTracked

            OnEditClicked -> EventAnalytics.Automatic("profile_edit_clicked")

            OnAvatarClicked -> EventAnalytics.Automatic("profile_photo_clicked", mapOf("source" to PROFILE_SOURCE))

            // Why: only a tap the account buttons take (online, read, no sign in nor sign out running).
            is OnProviderClicked -> EventAnalytics.Manual("sign_in_provider_clicked")

            OnNativeSignInFailed -> SignInAnalytics.buildNativeSignInFailed(PROFILE_SOURCE)

            OnNativeSignInCancelled -> SignInAnalytics.buildNativeSignInCancelled(PROFILE_SOURCE)

            OnRecoveryCodeClicked -> EventAnalytics.Automatic("profile_recovery_code_clicked")

            OnExportClicked -> EventAnalytics.Automatic("data_export_clicked", mapOf("source" to PROFILE_SOURCE))

            is OnBitcoinInSatsToggled -> EventAnalytics.Automatic("bitcoin_in_sats_toggled", mapOf("is_on" to isOn))

            OnNotificationsClicked -> EventAnalytics.Automatic("profile_notifications_clicked")

            OnAppInviteClicked -> EventAnalytics.Automatic("app_invite_code_clicked", mapOf("source" to PROFILE_SOURCE))

            // Why: only while the row is highlighted, which the ViewModel knows.
            OnAppInviteDismissed -> EventAnalytics.Manual("app_invite_dismissed")

            OnCategoryMapClicked -> EventAnalytics.Automatic("category_map_clicked", mapOf("source" to PROFILE_SOURCE))

            OnPrivacyPolicyClicked -> EventAnalytics.Automatic("privacy_policy_clicked")

            OnTermsClicked -> EventAnalytics.Automatic("terms_clicked")

            OnCancelScheduledSignOutClicked -> EventAnalytics.Automatic("sign_out_scheduled_cancel_clicked")

            is OnCodeSavedChanged -> EventAnalytics.Automatic(
                "erase_code_saved_toggled",
                mapOf("is_checked" to isChecked),
            )

            /*
             * Why: each counts only when the profile takes it (nothing signing out or unlinking, the dialog it answers
             * still open, the row or switch there to tap, another theme), never a late tap.
             */
            OnVisibilityInfoClicked -> EventAnalytics.Manual("profile_visibility_info_clicked")

            OnDialogDismissed -> EventAnalytics.Manual("profile_dialog_cancelled")

            OnConnectClicked -> EventAnalytics.Manual("profile_connect_clicked")

            is OnAddPaymentKeyClicked -> EventAnalytics.Manual("payment_key_add_clicked")

            is OnPaymentKeyClicked -> EventAnalytics.Manual("payment_key_clicked")

            OnAccountClicked -> EventAnalytics.Manual("profile_account_clicked")

            OnUnlinkConfirmed -> EventAnalytics.Manual("profile_unlink_confirmed")

            OnDeleteAccountClicked -> EventAnalytics.Manual("profile_delete_account_clicked")

            OnRetrySyncClicked -> EventAnalytics.Manual("sync_retry_clicked")

            is OnThemeSelected -> EventAnalytics.Manual("theme_selected")

            OnAppearanceClicked -> EventAnalytics.Manual("profile_appearance_clicked")

            OnDynamicColorsInfoClicked -> EventAnalytics.Manual("profile_dynamic_colors_info_clicked")

            is OnDynamicColorsToggled -> EventAnalytics.Manual("dynamic_colors_toggled")

            OnCheckForUpdatesClicked -> EventAnalytics.Manual("check_for_updates_clicked")

            OnSignOutClicked -> EventAnalytics.Manual("sign_out_clicked")

            OnSignOutConfirmed -> EventAnalytics.Manual("sign_out_confirmed")

            OnSignOutWhenSyncedClicked -> EventAnalytics.Manual("sign_out_when_synced_clicked")

            OnSignOutNowClicked -> EventAnalytics.Manual("sign_out_now_clicked")

            OnCopyCodeClicked -> EventAnalytics.Manual("erase_code_copy_clicked")

            OnConnectInsteadClicked -> EventAnalytics.Manual("erase_connect_instead_clicked")

            OnEraseConfirmed -> EventAnalytics.Manual("profile_erase_confirmed")
        }

    data object OnBackClicked : ProfileUiEvent

    data object OnEditClicked : ProfileUiEvent

    // Why: the avatar opens the photo big, apart from the rest of the card, which edits the profile.
    data object OnAvatarClicked : ProfileUiEvent

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
        // Why: Apple's native sheet only; the server trades it for the token it revokes when the account goes.
        val authorizationCode: String?,
    ) : ProfileUiEvent

    data object OnNativeSignInFailed : ProfileUiEvent

    data object OnNativeSignInCancelled : ProfileUiEvent

    // Why: each UI asks Pix or Bitcoin first, in its own native menu.
    data class OnAddPaymentKeyClicked(
        val method: PaymentMethod,
    ) : ProfileUiEvent

    data class OnPaymentKeyClicked(
        val keyId: String,
    ) : ProfileUiEvent

    data object OnRecoveryCodeClicked : ProfileUiEvent

    // Why: the Google or Apple row, which opens the "Desconectar" confirmation.
    data object OnAccountClicked : ProfileUiEvent

    data object OnUnlinkConfirmed : ProfileUiEvent

    data object OnExportClicked : ProfileUiEvent

    data object OnDeleteAccountClicked : ProfileUiEvent

    data object OnRetrySyncClicked : ProfileUiEvent

    data class OnThemeSelected(
        val theme: ThemePreference,
    ) : ProfileUiEvent

    data object OnAppearanceClicked : ProfileUiEvent

    data object OnDynamicColorsInfoClicked : ProfileUiEvent

    data class OnDynamicColorsToggled(
        val isOn: Boolean,
    ) : ProfileUiEvent

    data class OnBitcoinInSatsToggled(
        val isOn: Boolean,
    ) : ProfileUiEvent

    // Why: "App on your phone"'s "See code" (the Mac's "Show code…"), which opens the app's code (14.2).
    data object OnAppInviteClicked : ProfileUiEvent

    // Why: its ✕ while highlighted, which also takes Home's card away.
    data object OnAppInviteDismissed : ProfileUiEvent

    // Why: the "Notificações" row of Android and iOS; the web and macOS show the toggles in the profile itself.
    data object OnNotificationsClicked : ProfileUiEvent

    // Why: Preferências › "Mapa de categorias" (flow 9); the Mac opens it in its Ajustes instead.
    data object OnCategoryMapClicked : ProfileUiEvent

    data object OnPrivacyPolicyClicked : ProfileUiEvent

    data object OnTermsClicked : ProfileUiEvent

    // Why: "Buscar atualizações", in the place of "Versão" on Android, iOS and macOS.
    data object OnCheckForUpdatesClicked : ProfileUiEvent

    data object OnSignOutClicked : ProfileUiEvent

    data object OnSignOutConfirmed : ProfileUiEvent

    data object OnSignOutWhenSyncedClicked : ProfileUiEvent

    // Why: "Sair agora e perder as alterações", for a sync that may never succeed (offline for good, a rejected push).
    data object OnSignOutNowClicked : ProfileUiEvent

    data object OnCancelScheduledSignOutClicked : ProfileUiEvent

    data class OnCodeSavedChanged(
        val isChecked: Boolean,
    ) : ProfileUiEvent

    data object OnCopyCodeClicked : ProfileUiEvent

    // Why: "Conectar Google ou Apple em vez disso" in the erase dialog, which opens the account row's buttons.
    data object OnConnectInsteadClicked : ProfileUiEvent

    data object OnEraseConfirmed : ProfileUiEvent
}
