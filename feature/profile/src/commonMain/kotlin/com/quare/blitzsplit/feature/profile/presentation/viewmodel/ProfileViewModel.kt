package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingNotificationPreferenceChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingPaymentKeyChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingProfileChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.sync.connect.ProviderConnector
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.UnlinkAccount
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.feature.profile.presentation.mapper.toMaskedEmail
import com.quare.blitzsplit.feature.profile.presentation.mapper.toMaskedPaymentKey
import com.quare.blitzsplit.feature.profile.presentation.model.LegalLinks
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
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
    private val validatePaymentKey: ValidatePaymentKey,
    private val unlinkAccount: UnlinkAccount,
    private val exportUserData: ExportUserData,
    observeAccount: ObserveAccount,
    observeProfile: ObserveProfile,
    observePendingProfileChanges: ObservePendingProfileChanges,
    observePaymentKeys: ObservePaymentKeys,
    observePendingPaymentKeyChanges: ObservePendingPaymentKeyChanges,
    observeSyncState: ObserveSyncState,
    observeThemePreference: ObserveThemePreference,
    observeNotificationPreferences: ObserveNotificationPreferences,
    observePendingNotificationPreferenceChanges: ObservePendingNotificationPreferenceChanges,
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
                paymentKeys = emptyList(),
                isSignOutScheduled = false,
                theme = ThemePreference.SYSTEM,
                activeNotificationCount = NotificationPreferences.DEFAULT.enabledTypes.size,
                dialog = null,
                hasConfirmedCodeSaved = false,
                isConnectExpanded = false,
                connectingProvider = null,
                hasSignInFailed = false,
                isSigningOut = false,
                hasSignOutFailed = false,
                isUnlinking = false,
                hasUnlinkFailed = false,
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
            observePendingPaymentKeyChanges(),
            observePendingNotificationPreferenceChanges(),
            connectivityObserver.observe(),
        ) { syncState, hasPendingProfile, hasPendingKeys, hasPendingPreferences, isOnline ->
            uiState.update {
                it.copy(
                    isOnline = isOnline,
                    syncStatus = syncStatusOf(syncState),
                    pendingChanges = listOfNotNull(
                        PendingChange.PROFILE.takeIf { hasPendingProfile },
                        PendingChange.PAYMENT_KEYS.takeIf { hasPendingKeys },
                        PendingChange.NOTIFICATION_PREFERENCES.takeIf { hasPendingPreferences },
                    ),
                )
            }
        }.launchIn(viewModelScope)
        observePaymentKeys()
            .onEach { keys -> uiState.update { it.copy(paymentKeys = keys.mapNotNull(::toItem)) } }
            .launchIn(viewModelScope)
        observeThemePreference()
            .onEach { theme -> uiState.update { it.copy(theme = theme) } }
            .launchIn(viewModelScope)
        observeNotificationPreferences()
            .onEach { preferences ->
                uiState.update { it.copy(activeNotificationCount = preferences.enabledTypes.size) }
            }.launchIn(viewModelScope)
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
            is ProfileUiEvent.OnAddPaymentKeyClicked -> openPaymentKey(event.method, keyId = null)
            is ProfileUiEvent.OnPaymentKeyClicked -> openSavedPaymentKey(event.keyId)
            ProfileUiEvent.OnRecoveryCodeClicked -> showRecoveryCode()
            ProfileUiEvent.OnAccountClicked -> openUnlink()
            ProfileUiEvent.OnUnlinkConfirmed -> unlink()
            ProfileUiEvent.OnExportClicked -> export()
            ProfileUiEvent.OnDeleteAccountClicked -> openDeleteAccount()
            ProfileUiEvent.OnRetrySyncClicked -> retry()
            is ProfileUiEvent.OnThemeSelected -> selectTheme(event.theme)
            ProfileUiEvent.OnNotificationsClicked -> navigator.navigate(Route.NotificationSettings)
            ProfileUiEvent.OnCategoryMapClicked -> navigator.navigate(Route.CategoryMap)
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
                // Why: the account gone closes the unlink dialog with it, so it never asks about no provider.
                dialog = it.dialog.takeUnless { dialog -> dialog == ProfileDialog.UNLINK_ACCOUNT && connected == null },
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

    private fun openSavedPaymentKey(keyId: String) {
        val key = uiState.value.paymentKeys.find { it.id == keyId } ?: return
        openPaymentKey(key.type.method, keyId)
    }

    private fun openPaymentKey(
        method: PaymentMethod,
        keyId: String?,
    ) {
        navigator.navigate(
            Route.PaymentKey(
                method = method,
                keyId = keyId,
            ),
        )
    }

    // Why: the rules tell the format the row names ("· Taproot"); a key they reject never reaches here.
    private fun toItem(key: PaymentKey): PaymentKeyItem? {
        val validation = validatePaymentKey(key.type, key.value, otherKeys = emptyList())
        val format = (validation as? PaymentKeyValidation.Valid)?.format ?: return null
        return PaymentKeyItem(
            id = key.id,
            type = key.type,
            format = format,
            maskedValue = key.value.toMaskedPaymentKey(format),
            isDefault = key.isDefault,
        )
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

    // Why: a sign out or unlink under way keeps its dialog, which shows how it ends.
    private fun showDialog(dialog: ProfileDialog) {
        if (uiState.value.isSigningOut || uiState.value.isUnlinking) return
        uiState.update {
            it.copy(
                dialog = dialog,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
                hasUnlinkFailed = false,
            )
        }
    }

    private fun closeDialog() {
        if (uiState.value.isSigningOut || uiState.value.isUnlinking) return
        uiState.update {
            it.copy(
                dialog = null,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
                hasUnlinkFailed = false,
            )
        }
    }

    // Why: a sign out on its way would end the account the unlink keeps, so the row waits for it.
    private fun openUnlink() {
        val state = uiState.value
        if (state.hasAccount && !state.isSignOutScheduled) showDialog(ProfileDialog.UNLINK_ACCOUNT)
    }

    // Why: an unlink or sign out under way, or one scheduled, changes the account the deletion would name.
    private fun openDeleteAccount() {
        val state = uiState.value
        val isBusy = state.isUnlinking || state.isSigningOut || state.isSignOutScheduled
        if (state.showsDeleteAccount && !isBusy) {
            navigator.navigate(Route.DeleteAccount)
        }
    }

    /*
     * Why: the recovery code sheet shows once the account is gone, since its prompt comes due with the new
     * code, so a success only closes the dialog.
     */
    private fun unlink() {
        val state = uiState.value
        if (state.dialog != ProfileDialog.UNLINK_ACCOUNT || !state.canUnlink) return
        uiState.update {
            it.copy(
                isUnlinking = true,
                hasUnlinkFailed = false,
            )
        }
        viewModelScope.launch {
            val result = unlinkAccount()
            uiState.update {
                it.copy(
                    dialog = it.dialog.takeIf { result.isFailure },
                    isUnlinking = false,
                    hasUnlinkFailed = result.isFailure,
                )
            }
        }
    }

    private fun export() {
        // Why: the file is read from the device's copy, so a failure is one nobody can act on; the tap does nothing.
        viewModelScope.launch {
            exportUserData().onSuccess { json -> uiAction.emit(ProfileUiAction.SaveExport(json)) }
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
        viewModelScope.launch {
            providerConnector.connect(
                provider = provider,
                isFromProfile = true,
                launchNativeSignIn = ::launchNativeSignIn,
            )
        }
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
                authorizationCode = event.authorizationCode,
            )
        }
    }

    private fun emit(action: ProfileUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
