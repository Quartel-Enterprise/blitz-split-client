package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface SplitBillUiEvent : UiEvent {
    data class OnAmountChanged(
        val text: String,
    ) : SplitBillUiEvent

    data class OnDescriptionChanged(
        val text: String,
    ) : SplitBillUiEvent

    // Why: "Quem pagou", which opens the sheet.
    data object OnPayerClicked : SplitBillUiEvent

    data class OnPayerPicked(
        val memberId: String,
    ) : SplitBillUiEvent

    // Why: "Pronto" on the sheet.
    data object OnPayerDone : SplitBillUiEvent

    data object OnPayerDismissed : SplitBillUiEvent

    // Why: "Quando", picked in each platform's date and time picker.
    data class OnSpentAtChanged(
        val epochMillis: Long,
    ) : SplitBillUiEvent

    // Why: "Divisão", which opens "Dividir".
    data object OnSplitClicked : SplitBillUiEvent

    // Why: Igual, Valores, % or Partes.
    data class OnSplitModeClicked(
        val mode: SplitMode,
    ) : SplitBillUiEvent

    // Why: the checkbox of "Igual".
    data class OnPersonToggled(
        val memberId: String,
    ) : SplitBillUiEvent

    // Why: "Marcar todos" or "Desmarcar todos".
    data object OnEveryoneToggled : SplitBillUiEvent

    // Why: the field of "Valores" or "%".
    data class OnPersonValueChanged(
        val memberId: String,
        val text: String,
    ) : SplitBillUiEvent

    // Why: the stepper of "Partes".
    data class OnPartsIncreased(
        val memberId: String,
    ) : SplitBillUiEvent

    data class OnPartsDecreased(
        val memberId: String,
    ) : SplitBillUiEvent

    // Why: "Pronto" on "Dividir".
    data object OnSplitDone : SplitBillUiEvent

    // Why: "Voltar" or "Cancelar" on "Dividir", which leaves the bill's split as it was.
    data object OnSplitDismissed : SplitBillUiEvent

    data object OnSaveClicked : SplitBillUiEvent

    data object OnCloseClicked : SplitBillUiEvent
}
