package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
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

    // Why: Igual, Valores, % or Partes; Itens opens "Itens da conta" in Dividir's place.
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

    // Why: "Voltar ao padrão" on Dividir's notice, back to the group's "Divisão padrão" for this bill.
    data object OnSplitResetToDefault : SplitBillUiEvent

    // Why: "Pronto" on "Dividir".
    data object OnSplitDone : SplitBillUiEvent

    // Why: "Voltar" or "Cancelar" on "Dividir", which leaves the bill's split as it was.
    data object OnSplitDismissed : SplitBillUiEvent

    // Why: what "Itens da conta" (7.5), "Item" (7.6) and their questions send, which ItemsEditing handles.
    sealed interface Items : SplitBillUiEvent

    // Why: back on "Itens da conta", to Dividir, which keeps the items typed so far for Itens to open with again.
    data object OnItemsDismissed : Items

    // Why: "Pronto" on "Itens da conta", which splits the bill by its items and closes Dividir too.
    data object OnItemsDone : Items

    // Why: "Adicionar item", which opens "Item" for a new one.
    data object OnAddItemClicked : Items

    // Why: "Adicionar item de R$ 15,00" (7.5d), a new item priced at what the items fall short of.
    data object OnAddDifferenceItemClicked : Items

    // Why: "Dividir a diferença" (7.5d), shared in proportion to what each one consumed.
    data object OnSplitDifferenceClicked : Items

    // Why: the difference shared that way taken off again.
    data object OnDifferenceRemoved : Items

    // Why: an item of the list, which opens "Item" with it.
    data class OnItemClicked(
        val itemId: String,
    ) : Items

    data class OnItemNameChanged(
        val text: String,
    ) : Items

    // Why: the stepper of "Quantidade".
    data object OnItemQuantityIncreased : Items

    data object OnItemQuantityDecreased : Items

    // Why: "Quantidade" typed, on the web and the Mac.
    data class OnItemQuantityChanged(
        val text: String,
    ) : Items

    // Why: "Preço de cada".
    data class OnItemPriceChanged(
        val text: String,
    ) : Items

    // Why: Igual, Unidades, Partes, Valor or %.
    data class OnItemModeClicked(
        val mode: ItemSplitMode,
    ) : Items

    // Why: the checkbox of Igual.
    data class OnItemPersonToggled(
        val memberId: String,
    ) : Items

    // Why: the stepper of Unidades and Partes.
    data class OnItemPersonIncreased(
        val memberId: String,
    ) : Items

    data class OnItemPersonDecreased(
        val memberId: String,
    ) : Items

    // Why: the field of Valor and %, and the web's and the Mac's of Unidades and Partes.
    data class OnItemPersonValueChanged(
        val memberId: String,
        val text: String,
    ) : Items

    // Why: "Pronto" on "Item".
    data object OnItemDone : Items

    // Why: "Voltar" on "Item", which leaves the items as they were.
    data object OnItemDismissed : Items

    // Why: "Apagar item", from "Item" or the Mac's "−" under the list, which asks first.
    data class OnItemDeleteClicked(
        val itemId: String,
    ) : Items

    data object OnItemDeleteConfirmed : Items

    data object OnItemDeleteDismissed : Items

    // Why: the row of "Serviço" or "Couvert", which opens its field.
    data class OnFeeClicked(
        val fee: ItemsFee,
    ) : Items

    data class OnFeeValueChanged(
        val text: String,
    ) : Items

    data object OnFeeDone : Items

    data object OnFeeDismissed : Items

    data object OnSaveClicked : SplitBillUiEvent

    // Why: "Apagar conta" under the form of "Editar conta", which asks first.
    data object OnDeleteClicked : SplitBillUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : SplitBillUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : SplitBillUiEvent

    data object OnCloseClicked : SplitBillUiEvent
}
