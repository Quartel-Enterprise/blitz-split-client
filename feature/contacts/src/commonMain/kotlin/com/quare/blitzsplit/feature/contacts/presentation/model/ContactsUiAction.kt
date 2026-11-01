package com.quare.blitzsplit.feature.contacts.presentation.model

sealed interface ContactsUiAction {
    // Why: "Mostrar" couldn't be saved on the device, which is rare enough to just say so.
    data object ShowChangeError : ContactsUiAction
}
