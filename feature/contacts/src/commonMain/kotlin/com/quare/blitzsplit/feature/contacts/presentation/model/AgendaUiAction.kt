package com.quare.blitzsplit.feature.contacts.presentation.model

sealed interface AgendaUiAction {
    // Why: the system's own question about the contacts.
    data object RequestAccess : AgendaUiAction

    // Why: the app's page in the device's settings, the only way back once the access was refused.
    data object OpenSettings : AgendaUiAction
}
