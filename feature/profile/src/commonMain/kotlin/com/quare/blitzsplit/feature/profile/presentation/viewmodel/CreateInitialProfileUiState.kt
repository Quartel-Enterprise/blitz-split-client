package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import software.quare.loadable.Loadable

// Why: Profile before each source answered, which shimmers until isLoaded; only what the platform can do is known.
internal fun createInitialProfileUiState(
    showsDynamicColors: Boolean,
    showsCheckForUpdates: Boolean,
): ProfileUiState = ProfileUiState(
    isLoaded = false,
    displayName = "",
    avatarColor = AvatarColor.default,
    photoUrl = null,
    shareLink = null,
    accountProvider = null,
    hasAnonymousId = false,
    email = null,
    formattedRecoveryCode = null,
    showsRecoveryCodePrompt = false,
    isOnline = true,
    syncStatus = null,
    pendingChanges = emptyList(),
    paymentKeys = Loadable.Loading,
    showsPix = true,
    isSignOutScheduled = false,
    theme = ThemePreference.SYSTEM,
    showsDynamicColors = showsDynamicColors,
    usesDynamicColors = false,
    showsBitcoinInSats = false,
    language = AppLanguage.SYSTEM,
    activeNotificationCount = Loadable.Loading,
    showsCheckForUpdates = showsCheckForUpdates,
    isCheckingForUpdates = false,
    dialog = null,
    hasConfirmedCodeSaved = false,
    isConnectExpanded = false,
    connectingProvider = null,
    hasSignInFailed = false,
    isSigningOut = false,
    hasSignOutFailed = false,
    appInvite = null,
)
