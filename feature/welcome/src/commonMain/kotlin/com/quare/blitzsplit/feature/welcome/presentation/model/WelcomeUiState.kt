package com.quare.blitzsplit.feature.welcome.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

data class WelcomeUiState(
    val isOnline: Boolean,
    val connectingProvider: AccountProvider?,
    val error: WelcomeError?,
) {
    val areAccountButtonsEnabled: Boolean
        get() = isOnline && connectingProvider == null

    val showsOfflineNotice: Boolean
        get() = !isOnline
}
