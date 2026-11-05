package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "home"

sealed interface HomeUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnProfileClicked -> EventAnalytics.Automatic("profile_avatar_clicked", mapOf("source" to SOURCE))

            // Why: the web's logo, which opens Home; the tabs it stands beside are not tracked, their screen view is.
            OnBrandClicked -> EventAnalytics.Automatic("brand_clicked", mapOf("source" to SOURCE))

            // Why: only a search that opens, never ⌘K over the recovery code's prompt.
            OnSearchClicked -> EventAnalytics.Manual("search_opened")

            OnRetrySyncClicked -> EventAnalytics.Automatic("sync_retry_clicked", mapOf("source" to SOURCE))

            OnRefresh -> EventAnalytics.Automatic("pull_to_refresh", mapOf("source" to SOURCE))

            is OnPersonClicked -> EventAnalytics.Automatic("balance_face_clicked", mapOf("source" to SOURCE))

            OnSeeBalancesClicked -> EventAnalytics.Automatic("home_see_balances_clicked")

            OnToReceiveClicked -> EventAnalytics.Automatic("balances_to_receive_clicked", mapOf("source" to SOURCE))

            OnToPayClicked -> EventAnalytics.Automatic("balances_to_pay_clicked", mapOf("source" to SOURCE))

            OnSplitBillClicked -> EventAnalytics.Automatic("split_bill_clicked", mapOf("source" to SOURCE))

            OnScanReceiptClicked -> EventAnalytics.Automatic("scan_receipt_clicked", mapOf("source" to SOURCE))

            OnRecordPaymentClicked -> EventAnalytics.Automatic("record_payment_clicked", mapOf("source" to SOURCE))

            OnCreateGroupClicked -> EventAnalytics.Automatic("create_group_clicked", mapOf("source" to SOURCE))

            OnJoinWithLinkClicked -> EventAnalytics.Automatic("join_with_link_clicked", mapOf("source" to SOURCE))

            is OnGroupClicked -> EventAnalytics.Automatic("group_clicked", mapOf("source" to SOURCE))

            OnSeeAllGroupsClicked -> EventAnalytics.Automatic("home_see_all_groups_clicked")

            is OnBillClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to SOURCE))

            OnSeeAllBillsClicked -> EventAnalytics.Automatic("home_see_all_bills_clicked")

            // Why: only a tap that opens Atividade, never a late one over what Início opened.
            OnActivityClicked -> EventAnalytics.Manual("activity_bell_clicked")

            OnShowRecoveryCodeClicked -> EventAnalytics.Automatic("recovery_code_card_show_clicked")

            OnConnectAccountClicked -> EventAnalytics.Automatic("recovery_code_card_connect_clicked")

            OnRecoveryCodeCardDismissed -> EventAnalytics.Automatic("recovery_code_card_dismissed")

            OnUndoPaymentClicked -> EventAnalytics.Automatic("payment_undo_clicked", mapOf("source" to SOURCE))

            // Why: only while the card shows, with which one it was.
            OnNotificationPromptTurnOnClicked -> EventAnalytics.Manual("notification_prompt_turn_on_clicked")

            OnNotificationPromptDismissed -> EventAnalytics.Manual("notification_prompt_dismissed")

            // Why: a notice that went away, which nobody tapped.
            OnRecordedPaymentNoticeShown, OnDeletedGroupNoticeShown -> EventAnalytics.NotTracked

            // Why: what the system says about the permission: nobody tapped to act.
            is OnNotificationPermissionChecked -> EventAnalytics.NotTracked

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            OnGroupsTabClicked,
            OnBalancesTabClicked,
            OnContactsTabClicked,
            -> EventAnalytics.NotTracked
        }

    data object OnProfileClicked : HomeUiEvent

    // Why: the web's logo, which opens Home as its tab does.
    data object OnBrandClicked : HomeUiEvent

    // Why: "Buscar" (5.2), the same search as Grupos', over Início: the magnifier, the web's ⌘K.
    data object OnSearchClicked : HomeUiEvent

    // Why: the chip "Sem resposta do servidor · tentar de novo".
    data object OnRetrySyncClicked : HomeUiEvent

    // Why: pulled down on Android and iOS.
    data object OnRefresh : HomeUiEvent

    // Why: a face of "Saldos com pessoas": settling up with them (4.5) or Recebi (6.5), for the net.
    data class OnPersonClicked(
        val key: String,
        val currency: GroupCurrency,
    ) : HomeUiEvent

    // Why: "Ver saldos" over the faces.
    data object OnSeeBalancesClicked : HomeUiEvent

    // Why: "Te devem" and "Você deve" of "Saldo geral".
    data object OnToReceiveClicked : HomeUiEvent

    data object OnToPayClicked : HomeUiEvent

    // Why: "Rachar conta", Android's extended FAB, iOS's tinted glass item, the web's main button, the Mac's ⌘N.
    data object OnSplitBillClicked : HomeUiEvent

    // Why: the shortcuts: "Ler cupom fiscal", "Registrar pagamento" and "Novo grupo".
    data object OnScanReceiptClicked : HomeUiEvent

    data object OnRecordPaymentClicked : HomeUiEvent

    data object OnCreateGroupClicked : HomeUiEvent

    // Why: the first use's "Entrar com link ou QR".
    data object OnJoinWithLinkClicked : HomeUiEvent

    // Why: a card of "Seus grupos".
    data class OnGroupClicked(
        val groupId: String,
    ) : HomeUiEvent

    // Why: "Ver todos" over "Seus grupos".
    data object OnSeeAllGroupsClicked : HomeUiEvent

    // Why: a row of "Últimas despesas", which opens its Conta (7.3) over its group.
    data class OnBillClicked(
        val groupId: String,
        val expenseId: String,
    ) : HomeUiEvent

    // Why: "Ver tudo" over "Últimas despesas", which opens Atividade over Início, as the bell does.
    data object OnSeeAllBillsClicked : HomeUiEvent

    // Why: the bell in Início's bar, with Atividade's unread count, which opens Atividade over Início.
    data object OnActivityClicked : HomeUiEvent

    // Why: the card "Guarde seu código de recuperação": "Ver código", "Conectar conta" and ✕.
    data object OnShowRecoveryCodeClicked : HomeUiEvent

    data object OnConnectAccountClicked : HomeUiEvent

    data object OnRecoveryCodeCardDismissed : HomeUiEvent

    // Why: read on appear, on each return to the app and after the system's prompt, since the settings may change it.
    data class OnNotificationPermissionChecked(
        val permission: NotificationPermission,
    ) : HomeUiEvent

    // Why: the notification card: "Turn on" and "Not now".
    data object OnNotificationPromptTurnOnClicked : HomeUiEvent

    data object OnNotificationPromptDismissed : HomeUiEvent

    // Why: "Desfazer" on the notice of a payment just recorded from Início.
    data object OnUndoPaymentClicked : HomeUiEvent

    data object OnRecordedPaymentNoticeShown : HomeUiEvent

    data object OnDeletedGroupNoticeShown : HomeUiEvent

    data object OnGroupsTabClicked : HomeUiEvent

    data object OnBalancesTabClicked : HomeUiEvent

    data object OnContactsTabClicked : HomeUiEvent
}
