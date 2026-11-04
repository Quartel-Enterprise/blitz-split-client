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
            retrySync = get(),
            signOut = get(),
            signOutWhenSynced = get(),
            cancelSignOutWhenSynced = get(),
            validatePaymentKey = get(),
            unlinkAccount = get(),
            exportUserData = get(),
            observeAccount = get(),
            observeProfile = get(),
            observePendingProfileChanges = get(),
            observePaymentKeys = get(),
            observePendingPaymentKeyChanges = get(),
            observeSyncState = get(),
            observeThemePreference = get(),
            observeDynamicColors = get(),
            observeBitcoinInSats = get(),
            dynamicColorSupport = get(),
            observeNotificationPreferences = get(),
            observePendingNotificationPreferenceChanges = get(),
            observeFirstDownload = get(),
            connectivityObserver = get(),
        )
    }
}
