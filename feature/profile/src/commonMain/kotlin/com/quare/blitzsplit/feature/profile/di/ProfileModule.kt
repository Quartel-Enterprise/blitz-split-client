package com.quare.blitzsplit.feature.profile.di

import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Why: viewModelOf stops at 22 parameters, fewer than Perfil takes.
val profileModule = module {
    viewModel {
        ProfileViewModel(
            navigator = get(),
            providerConnector = get(),
            updateRecoveryCodePrompt = get(),
            updateThemePreference = get(),
            updateDynamicColors = get(),
            updateBitcoinInSats = get(),
            updateAppLanguage = get(),
            retrySync = get(),
            signOut = get(),
            signOutWhenSynced = get(),
            cancelSignOutWhenSynced = get(),
            validatePaymentKey = get(),
            exportUserData = get(),
            checkForUpdate = get(),
            showUpdatePrompt = get(),
            dismissAppInvite = get(),
            observeAppInvite = get(),
            updateMechanism = get(),
            clientPlatform = get(),
            observeAccount = get(),
            observeProfile = get(),
            observeProfileShareLink = get(),
            observePendingProfileChanges = get(),
            observePaymentKeys = get(),
            observePendingPaymentKeyChanges = get(),
            observeSyncState = get(),
            observeThemePreference = get(),
            observeDynamicColors = get(),
            observeBitcoinInSats = get(),
            observeAppLanguage = get(),
            dynamicColorSupport = get(),
            observeNotificationPreferences = get(),
            observePendingNotificationPreferenceChanges = get(),
            observePushEnabled = get(),
            observeFirstDownload = get(),
            connectivityObserver = get(),
            trackEvent = get(),
        )
    }
}
