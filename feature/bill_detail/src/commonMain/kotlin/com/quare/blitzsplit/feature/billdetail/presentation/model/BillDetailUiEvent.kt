package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface BillDetailUiEvent : UiEvent {
    data object OnBackClicked : BillDetailUiEvent

    // Why: "Editar conta" in the top bar.
    data object OnEditClicked : BillDetailUiEvent

    // Why: "Apagar conta" in the top bar, which asks first.
    data object OnDeleteClicked : BillDetailUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : BillDetailUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : BillDetailUiEvent
}
