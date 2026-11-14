package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ContactProfileUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back or a close, which each platform also does its own way: where it went is its screen view.
            OnBackClicked,
            OnAddToGroupDismissed,
            OnMergeDismissed,
            -> EventAnalytics.NotTracked

            // Why: "Em breve", which the tap always shows.
            OnAskAppUserClicked -> EventAnalytics.Automatic("contact_ask_app_user_clicked")

            /*
             * Why: each counts only when the screen takes it (someone still listed, the screen still on top or beside
             * Contatos, a question still open), never a late tap, as the ViewModel checks before acting.
             */
            OnChargeClicked -> EventAnalytics.Manual("charge_clicked")

            OnReceivedClicked -> EventAnalytics.Manual("received_clicked")

            OnSettleClicked -> EventAnalytics.Manual("settle_clicked")

            is OnGroupClicked -> EventAnalytics.Manual("group_clicked")

            OnAddToGroupClicked -> EventAnalytics.Manual("contact_add_to_group_clicked")

            is OnAddGroupPicked -> EventAnalytics.Manual("contact_add_to_group_picked")

            OnAddToGroupConfirmed -> EventAnalytics.Manual("contact_add_to_group_confirmed")

            OnHideClicked -> EventAnalytics.Manual("contact_hide_clicked")

            OnHideConfirmed -> EventAnalytics.Manual("contact_hide_confirmed")

            OnHideDismissed -> EventAnalytics.Manual("contact_hide_cancelled")

            OnShowClicked -> EventAnalytics.Manual("contact_show_clicked")

            OnViewBalancesClicked -> EventAnalytics.Manual("contact_view_balances_clicked")

            OnSeeAllHistoryClicked -> EventAnalytics.Manual("contact_history_see_all_clicked")

            OnInviteGuestClicked -> EventAnalytics.Manual("contact_invite_guest_clicked")

            OnMergeClicked -> EventAnalytics.Manual("contact_merge_clicked")

            OnClaimAsMeClicked -> EventAnalytics.Manual("contact_claim_clicked")

            OnClaimAsMeConfirmed -> EventAnalytics.Manual("contact_claim_confirmed")

            OnClaimAsMeDismissed -> EventAnalytics.Manual("contact_claim_cancelled")

            OnMergeRetryClicked -> EventAnalytics.Manual("retry_clicked")

            OnMergeCopyClicked -> EventAnalytics.Manual("invite_link_copy_clicked")

            OnMergeShareClicked -> EventAnalytics.Manual("invite_link_share_clicked")

            OnCancelMergeClicked -> EventAnalytics.Manual("contact_merge_cancel_clicked")

            OnShareProfileClicked -> EventAnalytics.Manual("contact_profile_share_clicked")
        }

    data object OnBackClicked : ContactProfileUiEvent

    // Why: "Cobrar", Saldos' message (8.2) for the person.
    data object OnChargeClicked : ContactProfileUiEvent

    // Why: "Recebi", Registrar pagamento (6.5) for the whole net.
    data object OnReceivedClicked : ContactProfileUiEvent

    // Why: "Acertar" (4.5), Registrar pagamento for the whole net, the other way.
    data object OnSettleClicked : ContactProfileUiEvent

    // Why: a group's badge or name in "Grupos em comum", which opens the group.
    data class OnGroupClicked(
        val groupId: String,
    ) : ContactProfileUiEvent

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

    // Why: "Eu sou Diego" on Juntar's sheet: the person using the app is the guest, so it asks first.
    data object OnClaimAsMeClicked : ContactProfileUiEvent

    data object OnClaimAsMeConfirmed : ContactProfileUiEvent

    data object OnClaimAsMeDismissed : ContactProfileUiEvent

    // Why: "Pedir para quem já usa o app", the other way to accept, not built yet.
    data object OnAskAppUserClicked : ContactProfileUiEvent

    // Why: "Cancelar" on "Aguardando Diego aceitar".
    data object OnCancelMergeClicked : ContactProfileUiEvent

    // Why: "Share profile" in the bar, the person's profile link, as they would share it from their own Profile.
    data object OnShareProfileClicked : ContactProfileUiEvent
}
