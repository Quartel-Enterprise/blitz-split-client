package com.quare.blitzsplit.feature.deleteaccount.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "delete_account"

sealed interface DeleteAccountUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: Cancelar, the back arrow and a native dismiss all leave without deleting.
            OnBackClicked -> EventAnalytics.Automatic("delete_account_cancelled")

            // Why: only a change the screen takes, never one offline or while deleting.
            is OnConfirmChanged -> EventAnalytics.Manual("delete_account_confirm_toggled")

            OnDeleteClicked -> EventAnalytics.Automatic("delete_account_clicked")

            OnExportClicked -> EventAnalytics.Automatic("data_export_clicked", mapOf("source" to SOURCE))
        }

    // Why: the back arrow, "Cancelar" and a native dismiss of the sheet or dialog.
    data object OnBackClicked : DeleteAccountUiEvent

    data class OnConfirmChanged(
        val isChecked: Boolean,
    ) : DeleteAccountUiEvent

    data object OnDeleteClicked : DeleteAccountUiEvent

    // Why: "Exportar meus dados antes", the same file as the profile's.
    data object OnExportClicked : DeleteAccountUiEvent
}
