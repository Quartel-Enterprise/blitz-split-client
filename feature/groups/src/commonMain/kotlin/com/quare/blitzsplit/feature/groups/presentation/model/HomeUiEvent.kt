package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.ReceiptPhotoSource
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says; the receipt camera's as Rachar conta's own.
private const val SOURCE = "home"
private const val RECEIPT_SOURCE = "receipt"

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

            // Why: only a tap that asks where the photo comes from (online, once Home is read).
            OnScanReceiptClicked -> EventAnalytics.Manual("scan_receipt_clicked")

            // Why: only a source picked on the choice still open, with which one.
            is OnReceiptSourcePicked -> EventAnalytics.Manual("receipt_source_picked")

            // Why: a photo handed back, which stands for the tap that opened its picker.
            is OnReceiptPhotoPicked -> EventAnalytics.Manual("photo_picked")

            // Why: a toggle, sent with where it ended up.
            is OnReceiptTorchClicked -> EventAnalytics.Automatic(
                "receipt_camera_torch_toggled",
                mapOf("is_on" to isOn),
            )

            OnReceiptCameraAllowClicked -> EventAnalytics.Automatic(
                "camera_allow_clicked",
                mapOf("source" to RECEIPT_SOURCE),
            )

            OnReceiptCameraSettingsClicked -> EventAnalytics.Automatic(
                "camera_settings_clicked",
                mapOf("source" to RECEIPT_SOURCE),
            )

            /*
             * Why: no pick, only the device's lack of a camera, and a close: the tap was "Ler cupom fiscal", counted on
             * its own, and Rachar conta's screen view says a photo came.
             */
            is OnReceiptChoiceSkipped,
            OnReceiptPickerDismissed,
            -> EventAnalytics.NotTracked

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

            // Why: only while the card shows, which the ViewModel knows.
            OnAppInviteDismissed -> EventAnalytics.Manual("app_invite_dismissed")

            OnAppInviteDownloadedClicked -> EventAnalytics.Manual("app_invite_downloaded_clicked")

            OnAppInviteCodeClicked -> EventAnalytics.Automatic("app_invite_code_clicked", mapOf("source" to SOURCE))

            OnAppInviteConnectClicked -> EventAnalytics.Automatic("app_invite_connect_clicked")

            OnAppBannerDismissed -> EventAnalytics.Automatic("app_banner_dismissed")

            OnAppInviteLinkClicked -> EventAnalytics.Automatic("app_link_clicked", mapOf("source" to SOURCE))

            OnAppBannerOpenClicked -> EventAnalytics.Automatic("app_link_clicked", mapOf("source" to "home_banner"))

            // Why: only while the card shows, with which one it was.
            OnNotificationPromptTurnOnClicked -> EventAnalytics.Manual("notification_prompt_turn_on_clicked")

            OnNotificationPromptDismissed -> EventAnalytics.Manual("notification_prompt_dismissed")

            /*
             * Why: what the system says after the quiet request, which nobody tapped, but which may bring up the card
             * (counted as shown) or answer a "Turn on" still waiting.
             */
            is OnQuietNotificationsAnswered -> EventAnalytics.Manual(
                setOf("notification_permission_answered", "notification_prompt_shown"),
            )

            // Why: a notice that went away, which nobody tapped.
            OnRecordedPaymentNoticeShown, OnDeletedGroupNoticeShown -> EventAnalytics.NotTracked

            /*
             * Why: what the system says, which nobody tapped, but it ends the funnel the card's "Turn on" began, so the
             * first check after it sends the answer. The card it lets come up (it waits for a known permission) sends
             * that it shows, from the state, once per showing whatever brought it.
             */
            is OnNotificationPermissionChecked -> EventAnalytics.Manual(
                setOf("notification_permission_answered", "notification_prompt_shown"),
            )

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

    // Why: what "Ler cupom fiscal" sends, which HomeReceiptSource handles (beta feedback, part 3, item 16).
    sealed interface Receipt : HomeUiEvent

    /*
     * Why: the shortcuts: "Ler cupom fiscal", "Registrar pagamento" and "Novo grupo". "Ler cupom fiscal" asks where the
     * photo comes from first, then opens Rachar conta reading it.
     */
    data object OnScanReceiptClicked : Receipt

    // Why: "Tirar foto", "Escolher da galeria" or "Escolher arquivo" on that choice, which opens its picker.
    data class OnReceiptSourcePicked(
        val source: ReceiptPhotoSource,
    ) : Receipt

    // Why: the choice asked for where there is no camera, so the gallery or the file chooser opens without asking.
    data class OnReceiptChoiceSkipped(
        val source: ReceiptPhotoSource,
    ) : Receipt

    // Why: the receipt's photo, from the camera, the gallery or a file, as it came; empty when it couldn't be read.
    class OnReceiptPhotoPicked(
        val photo: ByteArray,
    ) : Receipt

    // Why: the choice, the camera, the gallery or the file picker closed without a photo.
    data object OnReceiptPickerDismissed : Receipt

    // Why: the receipt camera's flashlight, with where it ended up; the camera is each platform's own.
    data class OnReceiptTorchClicked(
        val isOn: Boolean,
    ) : Receipt

    // Why: "Permitir câmera" on the receipt camera after a first refusal, which asks the system again (Android).
    data object OnReceiptCameraAllowClicked : Receipt

    // Why: "Abrir configurações" on the receipt camera once the camera was refused for good.
    data object OnReceiptCameraSettingsClicked : Receipt

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
        // Why: Apple only: it can deliver quietly (provisional) with no prompt while never answered.
        val canAskQuietly: Boolean,
    ) : HomeUiEvent

    // Why: Apple, the permission read right after its quiet request, which tells it from a read started before it.
    data class OnQuietNotificationsAnswered(
        val permission: NotificationPermission,
    ) : HomeUiEvent

    // Why: the notification card: "Turn on" and "Not now".
    data object OnNotificationPromptTurnOnClicked : HomeUiEvent

    data object OnNotificationPromptDismissed : HomeUiEvent

    // Why: the invite's card (14.1): ✕ ("Don't show again"), "I downloaded it", its QR code and "Connect account".
    data object OnAppInviteDismissed : HomeUiEvent

    data object OnAppInviteDownloadedClicked : HomeUiEvent

    data object OnAppInviteCodeClicked : HomeUiEvent

    data object OnAppInviteConnectClicked : HomeUiEvent

    // Why: the ✕ of the phone browser's banner (14.3).
    data object OnAppBannerDismissed : HomeUiEvent

    // Why: the app's link beside the card's QR code (web), which the browser opens itself; the shared code only counts it.
    data object OnAppInviteLinkClicked : HomeUiEvent

    // Why: "Open in the app" of the phone browser's banner, the app's link the browser opens itself.
    data object OnAppBannerOpenClicked : HomeUiEvent

    // Why: "Desfazer" on the notice of a payment just recorded from Início.
    data object OnUndoPaymentClicked : HomeUiEvent

    data object OnRecordedPaymentNoticeShown : HomeUiEvent

    data object OnDeletedGroupNoticeShown : HomeUiEvent

    data object OnGroupsTabClicked : HomeUiEvent

    data object OnBalancesTabClicked : HomeUiEvent

    data object OnContactsTabClicked : HomeUiEvent
}
