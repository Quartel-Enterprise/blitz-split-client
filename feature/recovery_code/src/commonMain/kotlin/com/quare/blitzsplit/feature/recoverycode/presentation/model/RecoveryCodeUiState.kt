package com.quare.blitzsplit.feature.recoverycode.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

data class RecoveryCodeUiState(
    val formattedCode: String?,
    val isPromptDue: Boolean,
    val isOnline: Boolean,
    val connectingProvider: AccountProvider?,
    val hasSignInFailed: Boolean,
    // Why: "Conectar Google ou Apple em vez disso" opens into one button per provider.
    val isConnectExpanded: Boolean,
) {
    val areAccountButtonsEnabled: Boolean
        get() = isOnline && connectingProvider == null
}
