package com.quare.blitzsplit.feature.deleteaccount.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

data class DeleteAccountUiState(
    // Why: "O vínculo com a conta Google" names it; null once the account is gone.
    val accountProvider: AccountProvider?,
    val isOnline: Boolean,
    // Why: "Entendo que não dá para desfazer", which unlocks the button.
    val hasConfirmed: Boolean,
    val isDeleting: Boolean,
    // Why: each UI says it in its own words, on the screen that stays open for another try.
    val hasFailed: Boolean,
) {
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: offline the checkbox dims with the button, since deleting needs the server.
    val isConfirmEnabled: Boolean
        get() = isOnline && !isDeleting

    val canDelete: Boolean
        get() = isOnline && hasConfirmed && !isDeleting && accountProvider != null
}
