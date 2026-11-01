package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ContactProfileUiEvent : UiEvent {
    data object OnBackClicked : ContactProfileUiEvent

    // Why: "Cobrar", Saldos' message (8.2) for the person.
    data object OnChargeClicked : ContactProfileUiEvent

    // Why: "Recebi", Registrar pagamento (6.5) for the whole net.
    data object OnReceivedClicked : ContactProfileUiEvent

    // Why: "Acertar" (4.5), Registrar pagamento for the whole net, the other way.
    data object OnSettleClicked : ContactProfileUiEvent

    data object OnAddToGroupClicked : ContactProfileUiEvent

    data class OnAddGroupPicked(
        val groupId: String,
    ) : ContactProfileUiEvent

    data object OnAddToGroupConfirmed : ContactProfileUiEvent

    data object OnAddToGroupDismissed : ContactProfileUiEvent

    // Why: "Ocultar de Contatos" in the ⋮ menu, which asks first.
    data object OnHideClicked : ContactProfileUiEvent

    data object OnHideConfirmed : ContactProfileUiEvent

    data object OnHideDismissed : ContactProfileUiEvent

    // Why: "Mostrar em Contatos" on someone hidden.
    data object OnShowClicked : ContactProfileUiEvent

    // Why: "Ver em Saldos" in the ⋮ menu.
    data object OnViewBalancesClicked : ContactProfileUiEvent

    // Why: "Ver tudo", Atividade filtered by the person (page 12).
    data object OnSeeAllHistoryClicked : ContactProfileUiEvent

    // Why: "Convidar Diego", the guest's personal link.
    data object OnInviteGuestClicked : ContactProfileUiEvent

    // Why: "Juntar com uma conta" on the guest card or "Juntar com a conta dele" in the menu.
    data object OnMergeClicked : ContactProfileUiEvent

    data object OnMergeRetryClicked : ContactProfileUiEvent

    data object OnMergeCopyClicked : ContactProfileUiEvent

    data object OnMergeShareClicked : ContactProfileUiEvent

    data object OnMergeDismissed : ContactProfileUiEvent

    // Why: "Pedir para quem já usa o app", the other way to accept, not built yet.
    data object OnAskAppUserClicked : ContactProfileUiEvent

    // Why: "Cancelar" on "Aguardando Diego aceitar".
    data object OnCancelMergeClicked : ContactProfileUiEvent
}
