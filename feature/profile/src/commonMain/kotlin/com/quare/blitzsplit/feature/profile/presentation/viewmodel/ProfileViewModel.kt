package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingProfileChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.sync.connect.ProviderConnector
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.feature.profile.presentation.mapper.toMaskedEmail
import com.quare.blitzsplit.feature.profile.presentation.model.LegalLinks
import com.quare.blitzsplit.feature.profile.presentation.model.PendingChange
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class ProfileViewModel(
    private val navigator: Navigator,
    private val providerConnector: ProviderConnector,
    private val updateRecoveryCodePrompt: UpdateRecoveryCodePrompt,
    private val updateThemePreference: UpdateThemePreference,
    private val retrySync: RetrySync,
    private val signOut: SignOut,
    private val signOutWhenSynced: SignOutWhenSynced,
    private val cancelSignOutWhenSynced: CancelSignOutWhenSynced,
    observeAccount: ObserveAccount,
    observeProfile: ObserveProfile,
    observePendingProfileChanges: ObservePendingProfileChanges,
    observeSyncState: ObserveSyncState,
    observeThemePreference: ObserveThemePreference,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState>
        field = MutableStateFlow(
            ProfileUiState(
                displayName = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                hasAnonymousId = false,
                maskedEmail = null,
                formattedRecoveryCode = null,
                showsRecoveryCodePrompt = false,
                isOnline = true,
                syncStatus = null,
                pendingChanges = emptyList(),
                isSignOutScheduled = false,
                theme = ThemePreference.SYSTEM,
                dialog = null,
                hasConfirmedCodeSaved = false,
                isConnectExpanded = false,
                connectingProvider = null,
                hasSignInFailed = false,
                isSigningOut = false,
                hasSignOutFailed = false,
            ),
        )

    val uiAction: SharedFlow<ProfileUiAction>
        field = MutableSharedFlow<ProfileUiAction>()

    private var recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE

    val stateWatcher: FlowWatcher<ProfileUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ProfileUiAction> = FlowWatcher(uiAction)

    init {
        observeProfile()
            .onEach { profile ->
                uiState.update {
                    it.copy(
                        displayName = profile?.displayName?.trim().orEmpty(),
                        avatarColor = profile?.avatarColor ?: AvatarColor.default,
                        photoUrl = profile?.photo?.toImageUrl(),
                    )
                }
            }.launchIn(viewModelScope)
        observeAccount()
            .onEach(::updateAccount)
            .launchIn(viewModelScope)
        combine(
            observeSyncState(),
            observePendingProfileChanges(),
            connectivityObserver.observe(),
        ) { syncState, hasPendingProfile, isOnline ->
            uiState.update {
                it.copy(
                    isOnline = isOnline,
                    syncStatus = syncStatusOf(syncState),
                    pendingChanges = if (hasPendingProfile) listOf(PendingChange.PROFILE) else emptyList(),
                )
            }
        }.launchIn(viewModelScope)
        observeThemePreference()
            .onEach { theme -> uiState.update { it.copy(theme = theme) } }
            .launchIn(viewModelScope)
        providerConnector.connection
            .onEach { connection ->
                uiState.update {
                    it.copy(
                        connectingProvider = connection.connectingProvider,
                        hasSignInFailed = connection.hasFailed,
                        isConnectExpanded = connection.isExpanded,
                    )
                }
            }.launchIn(viewModelScope)
        providerConnector
            .observeBrowserSignIn { account -> account.status is AccountStatus.Connected }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.OnBackClicked -> navigator.navigateBack()
            ProfileUiEvent.OnEditClicked -> navigator.navigate(Route.EditProfile)
            ProfileUiEvent.OnVisibilityInfoClicked -> showDialog(ProfileDialog.VISIBILITY_INFO)
            ProfileUiEvent.OnDialogDismissed -> closeDialog()
            ProfileUiEvent.OnConnectClicked -> expandConnect()
            is ProfileUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is ProfileUiEvent.OnIdTokenReceived -> handleIdToken(event)
            ProfileUiEvent.OnNativeSignInFailed -> providerConnector.finishConnecting(hasFailed = true)
            ProfileUiEvent.OnNativeSignInCancelled -> providerConnector.finishConnecting(hasFailed = false)
            ProfileUiEvent.OnRecoveryCodeClicked -> showRecoveryCode()
            ProfileUiEvent.OnRetrySyncClicked -> retry()
            is ProfileUiEvent.OnThemeSelected -> selectTheme(event.theme)
            ProfileUiEvent.OnPrivacyPolicyClicked -> emit(ProfileUiAction.OpenUrl(LegalLinks.PRIVACY_POLICY))
            ProfileUiEvent.OnTermsClicked -> emit(ProfileUiAction.OpenUrl(LegalLinks.TERMS_OF_USE))
            ProfileUiEvent.OnSignOutClicked -> showSignOutDialog()
            ProfileUiEvent.OnSignOutConfirmed -> if (uiState.value.dialog == ProfileDialog.SIGN_OUT) clearSession()
            ProfileUiEvent.OnSignOutWhenSyncedClicked -> scheduleSignOut()
            ProfileUiEvent.OnSignOutNowClicked -> signOutNow()
            ProfileUiEvent.OnCancelScheduledSignOutClicked -> cancelScheduledSignOut()
            is ProfileUiEvent.OnCodeSavedChanged -> confirmCodeSaved(event.isChecked)
            ProfileUiEvent.OnCopyCodeClicked -> copyCode()
            ProfileUiEvent.OnConnectInsteadClicked -> connectInstead()
            ProfileUiEvent.OnEraseConfirmed -> if (uiState.value.dialog == ProfileDialog.ERASE_PROFILE) eraseProfile()
        }
    }

    private fun updateAccount(account: AccountSnapshot) {
        recoveryCodePrompt = account.recoveryCodePrompt
        val connected = account.status as? AccountStatus.Connected
        uiState.update {
            it.copy(
                accountProvider = connected?.provider,
                hasAnonymousId = account.status is AccountStatus.Anonymous,
                maskedEmail = account.email?.takeIf { connected != null }?.toMaskedEmail(),
                formattedRecoveryCode = account.recoveryCode?.let(RecoveryCodeFormat::format),
                showsRecoveryCodePrompt = account.recoveryCodePrompt.isShown,
                isSignOutScheduled = account.signOutWhenSynced,
            )
        }
    }

    // Why: SYNCED draws nothing; PENDING waits for the connection, or for the next sync, which is imminent online.
    private fun syncStatusOf(syncState: SyncState): ProfileSyncStatus? = when (syncState) {
        SyncState.SYNCED -> null
        SyncState.PENDING -> ProfileSyncStatus.WAITING
        SyncState.SYNCING -> ProfileSyncStatus.SYNCING
        SyncState.FAILED -> ProfileSyncStatus.FAILED
    }

    /*
     * Why: without Google or Apple the profile only comes back with the recovery code, so erasing asks for it to be
     * kept. With an account, changes that haven't gone up would be lost, so the sign out waits for them.
     */
    private fun signOutDialogOf(state: ProfileUiState): ProfileDialog = when {
        !state.hasAccount -> ProfileDialog.ERASE_PROFILE
        state.pendingChangeCount > 0 -> ProfileDialog.SIGN_OUT_PENDING
        else -> ProfileDialog.SIGN_OUT
    }

    // Why: a code already saved opens for review, so closing it with "Lembrar depois" leaves it saved.
    private fun showRecoveryCode() {
        val isSaved = recoveryCodePrompt == RecoveryCodePrompt.SAVED
        val prompt = if (isSaved) RecoveryCodePrompt.REVIEW else RecoveryCodePrompt.DUE
        viewModelScope.launch { updateRecoveryCodePrompt(prompt) }
    }

    private fun retry() {
        if (uiState.value.canRetrySync) retrySync()
    }

    private fun selectTheme(theme: ThemePreference) {
        viewModelScope.launch { updateThemePreference(theme) }
    }

    private fun cancelScheduledSignOut() {
        viewModelScope.launch { cancelSignOutWhenSynced() }
    }

    private fun confirmCodeSaved(isChecked: Boolean) {
        uiState.update { it.copy(hasConfirmedCodeSaved = isChecked) }
    }

    private fun copyCode() {
        uiState.value.formattedRecoveryCode?.let { code -> emit(ProfileUiAction.CopyToClipboard(code)) }
    }

    private fun expandConnect() {
        if (uiState.value.canConnect) providerConnector.expand()
    }

    private fun connectInstead() {
        if (!uiState.value.canConnect) return
        uiState.update {
            it.copy(
                dialog = null,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
                isConnectExpanded = true,
            )
        }
        providerConnector.expand()
    }

    private fun showSignOutDialog() {
        val state = uiState.value
        if (!state.canSignOut) return
        showDialog(signOutDialogOf(state))
    }

    private fun showDialog(dialog: ProfileDialog) {
        uiState.update {
            it.copy(
                dialog = dialog,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
            )
        }
    }

    private fun closeDialog() {
        if (uiState.value.isSigningOut) return
        uiState.update {
            it.copy(
                dialog = null,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
            )
        }
    }

    private fun scheduleSignOut() {
        if (uiState.value.dialog != ProfileDialog.SIGN_OUT_PENDING) return
        signOutWhenSynced()
        uiState.update {
            it.copy(
                dialog = null,
                hasSignOutFailed = false,
            )
        }
    }

    private fun signOutNow() {
        if (uiState.value.dialog == ProfileDialog.SIGN_OUT_PENDING) clearSession()
    }

    private fun eraseProfile() {
        if (!uiState.value.canErase) return
        clearSession()
    }

    // Why: once the profile is gone the app returns to the welcome, so only a failure needs handling here.
    private fun clearSession() {
        if (uiState.value.isSigningOut) return
        uiState.update {
            it.copy(
                isSigningOut = true,
                hasSignOutFailed = false,
            )
        }
        viewModelScope.launch {
            signOut().onFailure {
                uiState.update {
                    it.copy(
                        isSigningOut = false,
                        hasSignOutFailed = true,
                    )
                }
            }
        }
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        // Why: sign out checks connectingProvider, so it is set at the tap, before the connector's copy lands.
        uiState.update {
            it.copy(
                connectingProvider = provider,
                hasSignInFailed = false,
            )
        }
        viewModelScope.launch { providerConnector.connect(provider, ::launchNativeSignIn) }
    }

    private suspend fun launchNativeSignIn(provider: AccountProvider) {
        uiAction.emit(ProfileUiAction.LaunchNativeSignIn(provider))
    }

    private fun handleIdToken(event: ProfileUiEvent.OnIdTokenReceived) {
        viewModelScope.launch {
            providerConnector.onIdTokenReceived(
                provider = event.provider,
                idToken = event.idToken,
                rawNonce = event.rawNonce,
                fullName = event.fullName,
            )
        }
    }

    private fun emit(action: ProfileUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
