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

    // Why: the group of a new bill, which opens "Rachar em qual grupo?" with it marked.
    data object OnGroupClicked : SplitBillUiEvent

    data class OnGroupPicked(
        val groupId: String,
    ) : SplitBillUiEvent

    // Why: "Continuar" on "Rachar em qual grupo?".
    data object OnGroupDone : SplitBillUiEvent

    // Why: the sheet closed without "Continuar", which leaves the bill in its group.
    data object OnGroupDismissed : SplitBillUiEvent

    // Why: "Quem pagou", which opens the sheet with whoever paid marked, or "Várias pessoas".
    data object OnPayerClicked : SplitBillUiEvent

    data class OnPayerPicked(
        val memberId: String,
    ) : SplitBillUiEvent

    // Why: "Pronto" on the sheet.
    data object OnPayerDone : SplitBillUiEvent

    data object OnPayerDismissed : SplitBillUiEvent

    // Why: "Várias pessoas" on "Quem pagou", which opens "Várias pessoas pagaram" in its place.
    data object OnSeveralPayersClicked : SplitBillUiEvent

    // Why: how much one person paid, typed on "Várias pessoas pagaram".
    data class OnPayerAmountChanged(
        val memberId: String,
        val text: String,
    ) : SplitBillUiEvent

    // Why: "Pronto" on "Várias pessoas pagaram".
    data object OnSeveralPayersDone : SplitBillUiEvent

    // Why: "Várias pessoas pagaram" closed without "Pronto", which leaves who paid as it was.
    data object OnSeveralPayersDismissed : SplitBillUiEvent

    // Why: "Quando" opening its picker, which then stops at the day it is now, also once midnight passed meanwhile.
    data object OnSpentAtOpened : SplitBillUiEvent

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

    // Why: "Apagar conta" under the form of "Editar conta", which asks first.
    data object OnDeleteClicked : SplitBillUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : SplitBillUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : SplitBillUiEvent

    data object OnCloseClicked : SplitBillUiEvent
}
