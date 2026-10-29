package com.quare.blitzsplit.feature.deleteaccount.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface DeleteAccountUiEvent : UiEvent {
    // Why: the back arrow, "Cancelar" and a native dismiss of the sheet or dialog.
    data object OnBackClicked : DeleteAccountUiEvent

    data class OnConfirmChanged(
        val isChecked: Boolean,
    ) : DeleteAccountUiEvent

    data object OnDeleteClicked : DeleteAccountUiEvent

    // Why: "Exportar meus dados antes", the same file as the profile's.
    data object OnExportClicked : DeleteAccountUiEvent
}
