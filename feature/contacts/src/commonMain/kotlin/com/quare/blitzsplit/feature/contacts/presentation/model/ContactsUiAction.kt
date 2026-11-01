package com.quare.blitzsplit.feature.contacts.presentation.model

sealed interface ContactsUiAction {
    // Why: Atividade (page 12) is not built yet.
    data object ShowComingSoon : ContactsUiAction

    // Why: "Mostrar" couldn't be saved on the device, which is rare enough to just say so.
    data object ShowChangeError : ContactsUiAction
}
