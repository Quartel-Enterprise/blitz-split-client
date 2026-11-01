package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GlobalBalancesUiEvent : UiEvent {
    // Why: "Tudo", "Você deve" or "Te devem".
    data class OnFilterChanged(
        val filter: BalancesFilter,
    ) : GlobalBalancesUiEvent

    // Why: "Por grupo" opened or closed on a row.
    data class OnPersonExpandToggled(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: a row picked in a list and detail, whose person the pane beside the list then shows.
    data class OnPersonSelected(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: "Acertar", someone you owe.
    data class OnSettleClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: "Recebi", someone who owes you paid outside the app.
    data class OnReceivedClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    data class OnChargeClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: "Incluir minha chave Pix".
    data class OnChargePixKeyToggled(
        val isIncluded: Boolean,
    ) : GlobalBalancesUiEvent

    // Why: the message as typed; the UIs send it once something was typed.
    data class OnChargeMessageChanged(
        val message: String,
    ) : GlobalBalancesUiEvent

    // Why: "Enviar", with the message as the UI shows it, written in its own words.
    data class OnChargeShareClicked(
        val message: String,
    ) : GlobalBalancesUiEvent

    // Why: "Copiar mensagem".
    data class OnChargeCopyClicked(
        val message: String,
    ) : GlobalBalancesUiEvent

    // Why: ✕, a swipe down or a click outside.
    data object OnChargeDismissed : GlobalBalancesUiEvent

    // Why: the empty state's button: "Ver quem te deve", "Ver quem você deve" or "Ver grupos".
    data object OnEmptyActionClicked : GlobalBalancesUiEvent

    data object OnHomeTabClicked : GlobalBalancesUiEvent

    data object OnGroupsTabClicked : GlobalBalancesUiEvent

    data object OnContactsTabClicked : GlobalBalancesUiEvent

    data object OnActivityTabClicked : GlobalBalancesUiEvent

    data object OnProfileClicked : GlobalBalancesUiEvent

    // Why: "Desfazer" on the notice of a settling up just recorded.
    data object OnUndoPaymentClicked : GlobalBalancesUiEvent

    // Why: the notice went away without Desfazer.
    data object OnRecordedPaymentNoticeShown : GlobalBalancesUiEvent
}
