package com.quare.blitzsplit.feature.deleteaccount.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded
import software.quare.loadable.valueOrNull

data class DeleteAccountUiState(
    /*
     * Why: "O vínculo com a conta Google" names it; null once the account is gone. Loading until the account was
     * read, so "o que será apagado" never leaves the provider out before it.
     */
    val accountProvider: Loadable<AccountProvider?>,
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
        get() = accountProvider.isLoaded() && isOnline && !isDeleting

    val canDelete: Boolean
        get() = isOnline && hasConfirmed && !isDeleting && accountProvider.valueOrNull() != null
}
