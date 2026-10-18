package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

data class RestoreUiState(
    val code: String,
    val isCodeComplete: Boolean,
    val isOnline: Boolean,
    val isRestoring: Boolean,
    val connectingProvider: AccountProvider?,
    val error: RestoreError?,
) {
    val isBusy: Boolean
        get() = isRestoring || connectingProvider != null

    val areAccountButtonsEnabled: Boolean
        get() = isOnline && !isBusy

    val canRestore: Boolean
        get() = isOnline && isCodeComplete && !isBusy

    val showsOfflineNotice: Boolean
        get() = !isOnline
}
