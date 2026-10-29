package com.quare.blitzsplit.feature.deleteaccount.presentation.model

sealed interface DeleteAccountUiAction {
    // Why: saved like the profile's export, in a file each platform names in its own words.
    data class SaveExport(
        val json: String,
    ) : DeleteAccountUiAction
}
