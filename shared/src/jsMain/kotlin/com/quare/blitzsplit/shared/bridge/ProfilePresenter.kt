@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.mp.KoinPlatform

class JsProfileState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    val maskedEmail: String?,
    val formattedRecoveryCode: String?,
    val showsRecoveryCodeRow: Boolean,
    val showsRecoveryCodePrompt: Boolean,
    val showsOfflineNotice: Boolean,
    // Why: the ProfileSyncStatus's name, null when everything is synced.
    val syncStatus: String?,
    val showsProfilePending: Boolean,
    val pendingChangeCount: Int,
    val canRetrySync: Boolean,
    val isSignOutScheduled: Boolean,
    val theme: String,
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
)

class JsProfileAction(
    // Why: "copy" with the code, or "open" with a URL.
    val type: String,
    val value: String,
)

class ProfilePresenter {
    private val holder = ViewModelHolder(ProfileViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsProfileState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsProfileState(
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                accountProvider = state.accountProvider?.code,
                maskedEmail = state.maskedEmail,
                formattedRecoveryCode = state.formattedRecoveryCode,
                showsRecoveryCodeRow = state.showsRecoveryCodeRow,
                showsRecoveryCodePrompt = state.showsRecoveryCodePrompt,
                showsOfflineNotice = state.showsOfflineNotice,
                syncStatus = state.syncStatus?.name,
                showsProfilePending = state.showsProfilePending,
                pendingChangeCount = state.pendingChangeCount,
                canRetrySync = state.canRetrySync,
                isSignOutScheduled = state.isSignOutScheduled,
                theme = state.theme.code,
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
                is ProfileUiAction.LaunchNativeSignIn -> null
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(ProfileUiEvent.OnBackClicked)
    }

    fun visibilityInfo() {
        viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)
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

    fun recoveryCode() {
        viewModel.onEvent(ProfileUiEvent.OnRecoveryCodeClicked)
    }

    fun retrySync() {
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)
    }

    fun selectTheme(themeCode: String) {
        val theme = ThemePreference.fromCodeOrNull(themeCode) ?: return
        viewModel.onEvent(ProfileUiEvent.OnThemeSelected(theme))
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
