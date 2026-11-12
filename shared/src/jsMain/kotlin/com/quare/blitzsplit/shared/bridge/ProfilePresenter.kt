@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.mp.KoinPlatform
import software.quare.loadable.valueOrNull

class JsProfileState(
    // Why: false until the profile, the account and the theme were read, which draws the shimmer.
    val isLoaded: Boolean,
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "Share profile"'s link, null while no account owns the profile (the button hides).
    val shareLink: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    val hasAccount: Boolean,
    // Why: "Erase profile and sign out" in the place of "Sign out", and its dialog, when nothing brings it back.
    val erasesProfileOnSignOut: Boolean,
    val maskedEmail: String?,
    val formattedRecoveryCode: String?,
    val hasRecoveryCode: Boolean,
    val showsRecoveryCodeRow: Boolean,
    val showsRecoveryCodePrompt: Boolean,
    val showsOfflineNotice: Boolean,
    // Why: the ProfileSyncStatus's name, null when everything is synced.
    val syncStatus: String?,
    val showsProfilePending: Boolean,
    val pendingChangeCount: Int,
    val hasPendingChanges: Boolean,
    val canRetrySync: Boolean,
    val isSignOutScheduled: Boolean,
    val theme: String,
    // Why: Preferências' "Bitcoin em satoshis".
    val showsBitcoinInSats: Boolean,
    // Why: Preferences' "Language": "system", or the tag of the language picked ("en", "pt-BR").
    val language: String,
    // Why: the codes of the options, in the order every UI lists them.
    val languageOptions: Array<String>,
    // Why: null while it loads, also while a restored account's choices haven't come down.
    val activeNotificationCount: Int?,
    // Why: the ProfileDialog's name, null while none is open.
    val dialog: String?,
    val hasConfirmedCodeSaved: Boolean,
    val canErase: Boolean,
    val isConnectExpanded: Boolean,
    val areAccountButtonsEnabled: Boolean,
    val canConnect: Boolean,
    val showsConnectButtons: Boolean,
    val canSignOut: Boolean,
    val connectingProvider: String?,
    val hasSignInFailed: Boolean,
    val isSigningOut: Boolean,
    val hasSignOutFailed: Boolean,
    val showsDeleteAccount: Boolean,
    // Why: Pix first, each method's default first; null while they load (shimmer), never "no key" before it.
    val paymentKeys: Array<JsPaymentKeyItem>?,
    val canAddPaymentKey: Boolean,
    // Why: the Pix rule (docs/product/general.md): false collapses the Pix keys behind "Add a Pix key".
    val showsPix: Boolean,
    /*
     * Why: "App on your phone" (14.4): ProfileAppInviteStatus's name, "ACTIVE", "DISMISSED" or "IN_USE"; null where it
     * never shows.
     */
    val appInviteStatus: String?,
    // Why: what a phone's browser opens from the row instead of the code: the link that picks the phone's store.
    val appInviteLink: String?,
)

class JsPaymentKeyItem(
    val id: String,
    // Why: the PaymentKeyType's and the PaymentKeyFormat's names.
    val type: String,
    val format: String,
    val maskedValue: String,
    val isDefault: Boolean,
)

class JsProfileAction(
    /*
     * Why: "copy" with the code, "open" with a URL, "export" with the JSON to save, "reload" for a language picked,
     * "shareProfile" with the profile link (navigator.share, or a copy) or "copyProfileLink" with it.
     */
    val type: String,
    val value: String,
)

class ProfilePresenter {
    private val holder = ViewModelHolder(ProfileViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsProfileState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsProfileState(
                isLoaded = state.isLoaded,
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                shareLink = state.shareLink,
                accountProvider = state.accountProvider?.code,
                hasAccount = state.hasAccount,
                erasesProfileOnSignOut = state.erasesProfileOnSignOut,
                maskedEmail = state.maskedEmail,
                formattedRecoveryCode = state.formattedRecoveryCode,
                hasRecoveryCode = state.hasRecoveryCode,
                showsRecoveryCodeRow = state.showsRecoveryCodeRow,
                showsRecoveryCodePrompt = state.showsRecoveryCodePrompt,
                showsOfflineNotice = state.showsOfflineNotice,
                syncStatus = state.syncStatus?.name,
                showsProfilePending = state.showsProfilePending,
                pendingChangeCount = state.pendingChangeCount,
                hasPendingChanges = state.hasPendingChanges,
                canRetrySync = state.canRetrySync,
                isSignOutScheduled = state.isSignOutScheduled,
                theme = state.theme.code,
                showsBitcoinInSats = state.showsBitcoinInSats,
                language = state.language.code,
                languageOptions = state.languageOptions.map { it.code }.toTypedArray(),
                activeNotificationCount = state.activeNotificationCount.valueOrNull(),
                dialog = state.dialog?.name,
                hasConfirmedCodeSaved = state.hasConfirmedCodeSaved,
                canErase = state.canErase,
                isConnectExpanded = state.isConnectExpanded,
                areAccountButtonsEnabled = state.areAccountButtonsEnabled,
                canConnect = state.canConnect,
                showsConnectButtons = state.showsConnectButtons,
                canSignOut = state.canSignOut,
                connectingProvider = state.connectingProvider?.code,
                hasSignInFailed = state.hasSignInFailed,
                isSigningOut = state.isSigningOut,
                hasSignOutFailed = state.hasSignOutFailed,
                showsDeleteAccount = state.showsDeleteAccount,
                appInviteStatus = state.appInvite?.status?.name,
                appInviteLink = state.appInvite?.link,
                paymentKeys = state.paymentKeys
                    .valueOrNull()
                    ?.map { key ->
                        JsPaymentKeyItem(
                            id = key.id,
                            type = key.type.name,
                            format = key.format.name,
                            maskedValue = key.maskedValue,
                            isDefault = key.isDefault,
                        )
                    }?.toTypedArray(),
                canAddPaymentKey = state.canAddPaymentKey,
                showsPix = state.showsPix,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsProfileAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.mapNotNull { action ->
            when (action) {
                is ProfileUiAction.CopyToClipboard -> JsProfileAction("copy", action.formattedCode)

                is ProfileUiAction.OpenUrl -> JsProfileAction("open", action.url)

                is ProfileUiAction.SaveExport -> JsProfileAction("export", action.json)

                is ProfileUiAction.LaunchNativeSignIn -> null

                is ProfileUiAction.ShareProfileLink -> JsProfileAction("shareProfile", action.link)

                is ProfileUiAction.CopyProfileLink -> JsProfileAction("copyProfileLink", action.link)

                // Why: the page reads its language once, before it draws; the reload comes back to the profile.
                ProfileUiAction.ApplyLanguage -> {
                    keepProfileForLanguage()
                    JsProfileAction("reload", "")
                }

                // Why: the web never checks for updates, since it always serves the latest version.
                is ProfileUiAction.ShowUpdateCheckResult -> null
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(ProfileUiEvent.OnBackClicked)
    }

    fun shareProfile() {
        viewModel.onEvent(ProfileUiEvent.OnShareProfileClicked)
    }

    fun copyProfileLink() {
        viewModel.onEvent(ProfileUiEvent.OnCopyProfileLinkClicked)
    }

    fun dismissDialog() {
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)
    }

    fun connect() {
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)
    }

    fun clickProvider(providerCode: String) {
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(providerCode.toAccountProvider()))
    }

    fun addPaymentKey(methodCode: String) {
        val method = PaymentMethod.entries.find { it.name == methodCode } ?: return
        viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(method))
    }

    fun clickPaymentKey(keyId: String) {
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked(keyId))
    }

    fun recoveryCode() {
        viewModel.onEvent(ProfileUiEvent.OnRecoveryCodeClicked)
    }

    fun export() {
        viewModel.onEvent(ProfileUiEvent.OnExportClicked)
    }

    fun deleteAccount() {
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)
    }

    fun retrySync() {
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)
    }

    fun selectTheme(themeCode: String) {
        val theme = ThemePreference.fromCodeOrNull(themeCode) ?: return
        viewModel.onEvent(ProfileUiEvent.OnThemeSelected(theme))
    }

    fun selectLanguage(languageCode: String) {
        val language = AppLanguage.entries.firstOrNull { it.code == languageCode } ?: return
        viewModel.onEvent(ProfileUiEvent.OnLanguageSelected(language))
    }

    fun toggleBitcoinInSats(isOn: Boolean) {
        viewModel.onEvent(ProfileUiEvent.OnBitcoinInSatsToggled(isOn))
    }

    // Why: "App on your phone"'s "See code" and its ✕.
    fun openAppCode() {
        viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)
    }

    fun dismissAppInvite() {
        viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)
    }

    // Why: "Download" or "Open in the app" in a phone's browser, which the browser opens.
    fun trackAppLink() {
        viewModel.onEvent(ProfileUiEvent.OnAppLinkClicked)
    }

    fun categoryMap() {
        viewModel.onEvent(ProfileUiEvent.OnCategoryMapClicked)
    }

    fun privacyPolicy() {
        viewModel.onEvent(ProfileUiEvent.OnPrivacyPolicyClicked)
    }

    fun terms() {
        viewModel.onEvent(ProfileUiEvent.OnTermsClicked)
    }

    fun signOut() {
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
    }

    fun confirmSignOut() {
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
    }

    fun signOutWhenSynced() {
        viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)
    }

    fun signOutNow() {
        viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)
    }

    fun cancelScheduledSignOut() {
        viewModel.onEvent(ProfileUiEvent.OnCancelScheduledSignOutClicked)
    }

    fun codeSavedChanged(isChecked: Boolean) {
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(isChecked))
    }

    fun copyCode() {
        viewModel.onEvent(ProfileUiEvent.OnCopyCodeClicked)
    }

    fun connectInstead() {
        viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)
    }

    fun confirmErase() {
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
    }

    fun dispose() {
        holder.clear()
    }
}

// Why: "system" for System, as the theme's code, or the language's tag.
private val AppLanguage.code: String
    get() = tag ?: "system"
