package com.quare.blitzsplit.android.feature.profile

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
import com.quare.blitzsplit.feature.profile.presentation.model.PendingChange
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import software.quare.loadable.toLoaded

// Why: the state every preview of Perfil starts from, a Google account with keys of both methods.
internal fun createPreviewProfileState(
    accountProvider: AccountProvider? = AccountProvider.GOOGLE,
    isOnline: Boolean = true,
    syncStatus: ProfileSyncStatus? = null,
): ProfileUiState = ProfileUiState(
    displayName = "Pierre Vieira",
    avatarColor = AvatarColor.BLUE,
    photoUrl = null,
    shareLink = "https://blitzsplit.com/u/7Hk2pQ8xZa",
    accountProvider = accountProvider,
    hasAnonymousId = true,
    maskedEmail = accountProvider?.let { "p•••@gmail.com" },
    formattedRecoveryCode = if (accountProvider == null) "K7QM-4XPA-9TRE" else null,
    showsRecoveryCodePrompt = false,
    isOnline = isOnline,
    syncStatus = syncStatus,
    pendingChanges = if (syncStatus == null) emptyList() else listOf(PendingChange.PROFILE),
    isLoaded = true,
    paymentKeys = listOf(
        PaymentKeyItem(
            id = "pix-phone",
            type = PaymentKeyType.PHONE,
            format = PaymentKeyFormat.PHONE,
            maskedValue = "+55 (11) 9••••-4321",
            isDefault = true,
        ),
        PaymentKeyItem(
            id = "pix-email",
            type = PaymentKeyType.EMAIL,
            format = PaymentKeyFormat.EMAIL,
            maskedValue = "pie•••@email.com",
            isDefault = false,
        ),
        PaymentKeyItem(
            id = "btc-lightning",
            type = PaymentKeyType.LIGHTNING,
            format = PaymentKeyFormat.LIGHTNING_ADDRESS,
            maskedValue = "pierre@carteira.app",
            isDefault = true,
        ),
        PaymentKeyItem(
            id = "btc-onchain",
            type = PaymentKeyType.ONCHAIN,
            format = PaymentKeyFormat.TAPROOT,
            maskedValue = "bc1p5d7r…sxg3297",
            isDefault = false,
        ),
    ).toLoaded(),
    showsPix = true,
    isSignOutScheduled = false,
    theme = ThemePreference.SYSTEM,
    showsDynamicColors = true,
    usesDynamicColors = false,
    showsBitcoinInSats = false,
    language = AppLanguage.SYSTEM,
    activeNotificationCount = 3.toLoaded(),
    showsCheckForUpdates = true,
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
