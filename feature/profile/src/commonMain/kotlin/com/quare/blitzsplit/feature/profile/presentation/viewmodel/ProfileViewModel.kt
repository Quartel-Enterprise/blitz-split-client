package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CheckForUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ShowUpdatePrompt
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingNotificationPreferenceChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingPaymentKeyChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingProfileChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfileShareLink
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePushEnabled
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.model.DynamicColorSupport
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateBitcoinInSats
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.sync.connect.ProviderConnector
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.feature.profile.presentation.mapper.toMaskedEmail
import com.quare.blitzsplit.feature.profile.presentation.mapper.toMaskedPaymentKey
import com.quare.blitzsplit.feature.profile.presentation.mapper.toProfileAppInvite
import com.quare.blitzsplit.feature.profile.presentation.model.LegalLinks
import com.quare.blitzsplit.feature.profile.presentation.model.PROFILE_SOURCE
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import com.quare.blitzsplit.feature.profile.presentation.model.UpdateCheckResult
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
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

class ProfileViewModel(
    private val navigator: Navigator,
    private val providerConnector: ProviderConnector,
    private val updateRecoveryCodePrompt: UpdateRecoveryCodePrompt,
    private val updateThemePreference: UpdateThemePreference,
    private val updateDynamicColors: UpdateDynamicColors,
    private val updateBitcoinInSats: UpdateBitcoinInSats,
    private val retrySync: RetrySync,
    private val signOut: SignOut,
    private val signOutWhenSynced: SignOutWhenSynced,
    private val cancelSignOutWhenSynced: CancelSignOutWhenSynced,
    private val validatePaymentKey: ValidatePaymentKey,
    private val exportUserData: ExportUserData,
    private val checkForUpdate: CheckForUpdate,
    private val showUpdatePrompt: ShowUpdatePrompt,
    dismissAppInvite: DismissAppInvite,
    observeAppInvite: ObserveAppInvite,
    updateMechanism: UpdateMechanism,
    observeAccount: ObserveAccount,
    observeProfile: ObserveProfile,
    observeProfileShareLink: ObserveProfileShareLink,
    observePendingProfileChanges: ObservePendingProfileChanges,
    observePaymentKeys: ObservePaymentKeys,
    observePendingPaymentKeyChanges: ObservePendingPaymentKeyChanges,
    observeSyncState: ObserveSyncState,
    observeThemePreference: ObserveThemePreference,
    observeDynamicColors: ObserveDynamicColors,
    observeBitcoinInSats: ObserveBitcoinInSats,
    observeAppLanguage: ObserveAppLanguage,
    updateAppLanguage: UpdateAppLanguage,
    dynamicColorSupport: DynamicColorSupport,
    observeNotificationPreferences: ObserveNotificationPreferences,
    observePendingNotificationPreferenceChanges: ObservePendingNotificationPreferenceChanges,
    observePushEnabled: ObservePushEnabled,
    observeFirstDownload: ObserveFirstDownload,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<ProfileUiEvent>(trackEvent) {
    val uiState: StateFlow<ProfileUiState>
        field = MutableStateFlow(
            createInitialProfileUiState(
                showsDynamicColors = dynamicColorSupport(),
                showsCheckForUpdates = updateMechanism != UpdateMechanism.NONE,
            ),
        )

    private val appInviteRow = ProfileAppInviteRow(uiState, navigator, dismissAppInvite, trackEvent, viewModelScope)

    // Why: Profile's dialogs and sheets, which open only while nothing signs out.
    private val dialogs = ProfileDialogs(uiState, trackEvent)

    private val languagePicker = ProfileLanguagePicker(uiState, updateAppLanguage, trackEvent, ::emit)

    private val shareLink = ProfileShareLink(uiState, trackEvent, ::emit)

    // Why: the theme a tap asked for, which the stored theme reaches only once written.
    private var requestedTheme: ThemePreference? = null

    val uiAction: SharedFlow<ProfileUiAction>
        field = MutableSharedFlow<ProfileUiAction>()

    private var recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE

    val stateWatcher: FlowWatcher<ProfileUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ProfileUiAction> = FlowWatcher(uiAction)

    init {
        // Why: isLoaded once the profile, account, theme, colors, bitcoin's unit and what waits to go up each answered.
        combine(
            observeProfile().onEach(::updateProfile),
            observeAccount().onEach(::updateAccount),
            observeThemePreference().onEach { theme -> uiState.update { it.copy(theme = theme) } },
            uiState.observeSyncSummary(
                syncState = observeSyncState(),
                hasPendingProfile = observePendingProfileChanges(),
                hasPendingKeys = observePendingPaymentKeyChanges(),
                hasPendingPreferences = observePendingNotificationPreferenceChanges(),
                isOnline = connectivityObserver.observe(),
            ),
            combine(observeDynamicColors(), observeBitcoinInSats()) { isDynamic, showsSats ->
                uiState.update {
                    it.copy(
                        usesDynamicColors = isDynamic,
                        showsBitcoinInSats = showsSats,
                    )
                }
            },
        ) { _, _, _, _, _ -> }
            .onEach { uiState.update { it.copy(isLoaded = true) } }
            .launchIn(viewModelScope)
        languagePicker.follow(observeAppLanguage, viewModelScope)
        val firstDownload = observeFirstDownload()
        // Why: a restored account's keys and notification choices only come with its first full sync.
        combine(observePaymentKeys(), firstDownload) { keys, download ->
            if (download != null) Loadable.Loading else keys.mapNotNull(::toItem).toLoaded()
        }.onEach { keys -> uiState.update { it.copy(paymentKeys = keys) } }
            .launchIn(viewModelScope)
        // Why: none is active while Notifications' "Push notifications" switch is off on this device.
        combine(
            observeNotificationPreferences(),
            observePushEnabled(),
            firstDownload,
        ) { preferences, isPushEnabled, download ->
            if (download != null) Loadable.Loading else (if (isPushEnabled) preferences.push.size else 0).toLoaded()
        }.onEach { count -> uiState.update { it.copy(activeNotificationCount = count) } }
            .launchIn(viewModelScope)
        observeProfileShareLink()
            .onEach { link -> uiState.update { it.copy(shareLink = link) } }
            .launchIn(viewModelScope)
        observeAppInvite()
            .onEach { invite ->
                val row = invite?.let(::toProfileAppInvite)
                uiState.update { it.copy(appInvite = row) }
                appInviteRow.onInviteChanged(row)
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

    override fun handleEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.OnBackClicked -> navigator.navigateBackFrom(Route.Profile)

            ProfileUiEvent.OnEditClicked -> navigator.navigate(Route.EditProfile)

            ProfileUiEvent.OnAppInviteClicked -> appInviteRow.openCode()

            // Why: the browser opens the link; the tap is only tracked.
            ProfileUiEvent.OnAppLinkClicked -> Unit

            ProfileUiEvent.OnAppInviteDismissed -> appInviteRow.dismiss()

            ProfileUiEvent.OnAvatarClicked -> navigator.navigate(Route.ProfilePhoto)

            ProfileUiEvent.OnShareProfileClicked -> shareLink.share()

            ProfileUiEvent.OnCopyProfileLinkClicked -> shareLink.copy()

            ProfileUiEvent.OnDialogDismissed -> dialogs.close()

            ProfileUiEvent.OnConnectClicked -> expandConnect()

            is ProfileUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)

            is ProfileUiEvent.OnIdTokenReceived -> handleIdToken(event)

            ProfileUiEvent.OnNativeSignInFailed -> providerConnector.finishConnecting(hasFailed = true)

            ProfileUiEvent.OnNativeSignInCancelled -> providerConnector.finishConnecting(hasFailed = false)

            is ProfileUiEvent.OnAddPaymentKeyClicked -> addPaymentKey(event.method)

            is ProfileUiEvent.OnPaymentKeyClicked -> openSavedPaymentKey(event.keyId)

            ProfileUiEvent.OnRecoveryCodeClicked -> showRecoveryCode()

            ProfileUiEvent.OnExportClicked -> export()

            ProfileUiEvent.OnDeleteAccountClicked -> openDeleteAccount()

            ProfileUiEvent.OnRetrySyncClicked -> retry()

            is ProfileUiEvent.OnThemeSelected -> selectTheme(event.theme)

            ProfileUiEvent.OnAppearanceClicked -> dialogs.openAppearance()

            ProfileUiEvent.OnLanguageClicked -> dialogs.openLanguage()

            is ProfileUiEvent.OnLanguageSelected -> languagePicker.select(event.language)

            ProfileUiEvent.OnDynamicColorsInfoClicked -> dialogs.openDynamicColorsInfo()

            is ProfileUiEvent.OnDynamicColorsToggled -> toggleDynamicColors(event.isOn)

            is ProfileUiEvent.OnBitcoinInSatsToggled -> toggleBitcoinInSats(event.isOn)

            ProfileUiEvent.OnNotificationsClicked -> navigator.navigate(Route.NotificationSettings)

            ProfileUiEvent.OnCategoryMapClicked -> navigator.navigate(Route.CategoryMap)

            ProfileUiEvent.OnPrivacyPolicyClicked -> emit(ProfileUiAction.OpenUrl(LegalLinks.PRIVACY_POLICY))

            ProfileUiEvent.OnTermsClicked -> emit(ProfileUiAction.OpenUrl(LegalLinks.TERMS_OF_USE))

            ProfileUiEvent.OnCheckForUpdatesClicked -> checkForUpdates()

            ProfileUiEvent.OnSignOutClicked -> dialogs.openSignOut()

            ProfileUiEvent.OnSignOutConfirmed -> if (uiState.value.dialog == ProfileDialog.SIGN_OUT) {
                clearSession { trackEvent("sign_out_confirmed", emptyMap()) }
            }

            ProfileUiEvent.OnSignOutWhenSyncedClicked -> scheduleSignOut()

            ProfileUiEvent.OnSignOutNowClicked -> signOutNow()

            ProfileUiEvent.OnCancelScheduledSignOutClicked -> cancelScheduledSignOut()

            is ProfileUiEvent.OnCodeSavedChanged -> confirmCodeSaved(event.isChecked)

            ProfileUiEvent.OnCopyCodeClicked -> copyCode()

            ProfileUiEvent.OnConnectInsteadClicked -> connectInstead()

            ProfileUiEvent.OnEraseConfirmed -> if (uiState.value.dialog == ProfileDialog.ERASE_PROFILE &&
                uiState.value.canErase
            ) {
                clearSession { trackEvent("profile_erase_confirmed", emptyMap()) }
            }
        }
    }

    /*
     * Why: Perfil's "Buscar atualizações", as Bible Planner's: an update found opens what opening the app would (Google
     * Play's flow, or "Atualização disponível"), even within the hour; nothing to offer says so.
     */
    private fun checkForUpdates() {
        if (!uiState.value.showsCheckForUpdates || uiState.value.isCheckingForUpdates) return
        trackEvent("check_for_updates_clicked", emptyMap())
        uiState.update { it.copy(isCheckingForUpdates = true) }
        viewModelScope.launch {
            val availability = checkForUpdate()
            uiState.update { it.copy(isCheckingForUpdates = false) }
            val result = when (availability) {
                is UpdateAvailability.Pending -> return@launch showUpdatePrompt(availability, UpdatePromptSource.MANUAL)
                UpdateAvailability.Downloading -> UpdateCheckResult.DOWNLOADING
                UpdateAvailability.NotAvailable -> UpdateCheckResult.UP_TO_DATE
                UpdateAvailability.CheckFailed -> UpdateCheckResult.FAILED
            }
            uiAction.emit(ProfileUiAction.ShowUpdateCheckResult(result))
        }
    }

    private fun updateProfile(profile: UserProfile?) {
        uiState.update {
            it.copy(
                displayName = profile?.displayName?.trim().orEmpty(),
                avatarColor = profile?.avatarColor ?: AvatarColor.default,
                photoUrl = profile?.photo?.toImageUrl(),
            )
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

    private fun addPaymentKey(method: PaymentMethod) {
        if (!uiState.value.canAddPaymentKey) return
        trackEvent("payment_key_add_clicked", mapOf("method" to method))
        openPaymentKey(method, keyId = null)
    }

    private fun openSavedPaymentKey(keyId: String) {
        val key = uiState.value.paymentKeys
            .valueOrNull()
            ?.find { it.id == keyId } ?: return
        trackEvent("payment_key_clicked", mapOf("method" to key.type.method))
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
        if (!uiState.value.canRetrySync) return
        trackEvent("sync_retry_clicked", mapOf("source" to PROFILE_SOURCE))
        retrySync()
    }

    // Why: compared with the theme last asked for, as the stored one comes back only once written.
    private fun selectTheme(theme: ThemePreference) {
        if (theme != (requestedTheme ?: uiState.value.theme)) trackEvent("theme_selected", mapOf("theme" to theme))
        requestedTheme = theme
        viewModelScope.launch { updateThemePreference(theme) }
    }

    private fun toggleBitcoinInSats(isOn: Boolean) {
        viewModelScope.launch { updateBitcoinInSats(isOn) }
    }

    // Why: a device that can't draw the wallpaper's scheme shows no switch, so it stores no choice either.
    private fun toggleDynamicColors(isOn: Boolean) {
        if (!uiState.value.showsDynamicColors) return
        trackEvent("dynamic_colors_toggled", mapOf("is_on" to isOn))
        viewModelScope.launch { updateDynamicColors(isOn) }
    }

    private fun cancelScheduledSignOut() {
        viewModelScope.launch { cancelSignOutWhenSynced() }
    }

    private fun confirmCodeSaved(isChecked: Boolean) {
        uiState.update { it.copy(hasConfirmedCodeSaved = isChecked) }
    }

    private fun copyCode() {
        val code = uiState.value.formattedRecoveryCode ?: return
        trackEvent("erase_code_copy_clicked", emptyMap())
        emit(ProfileUiAction.CopyToClipboard(code))
    }

    private fun expandConnect() {
        if (!uiState.value.canConnect) return
        trackEvent("profile_connect_clicked", emptyMap())
        providerConnector.expand()
    }

    private fun connectInstead() {
        if (!uiState.value.canConnect) return
        trackEvent("erase_connect_instead_clicked", emptyMap())
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

    // Why: a sign out under way, or one scheduled, changes the account the deletion would name.
    private fun openDeleteAccount() {
        val state = uiState.value
        val isBusy = state.isSigningOut || state.isSignOutScheduled
        if (state.showsDeleteAccount && !isBusy) {
            trackEvent("profile_delete_account_clicked", emptyMap())
            navigator.navigate(Route.DeleteAccount)
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
        trackEvent("sign_out_when_synced_clicked", emptyMap())
        signOutWhenSynced()
        uiState.update {
            it.copy(
                dialog = null,
                hasSignOutFailed = false,
            )
        }
    }

    private fun signOutNow() {
        if (uiState.value.dialog == ProfileDialog.SIGN_OUT_PENDING) {
            clearSession { trackEvent("sign_out_now_clicked", emptyMap()) }
        }
    }

    /*
     * Why: once the profile is gone the app returns to the welcome, so only a failure needs handling here. onStarted
     * counts the tap once the sign out is taken, before it runs, so its event comes before the welcome's screen view.
     */
    private fun clearSession(onStarted: () -> Unit) {
        if (uiState.value.isSigningOut) return
        onStarted()
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
        trackEvent("sign_in_provider_clicked", mapOf("provider" to provider, "source" to PROFILE_SOURCE))
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
