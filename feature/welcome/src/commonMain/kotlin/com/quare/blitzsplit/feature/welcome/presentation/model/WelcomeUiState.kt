package com.quare.blitzsplit.feature.welcome.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

data class WelcomeUiState(
    val isOnline: Boolean,
    val connectingProvider: AccountProvider?,
    val error: WelcomeError?,
    // Why: the account this device had was deleted after 30 days without a sync, which the welcome tells once.
    val showsExpiredAccountNotice: Boolean,
) {
    val areAccountButtonsEnabled: Boolean
        get() = isOnline && connectingProvider == null

    val showsOfflineNotice: Boolean
        get() = !isOnline
}
