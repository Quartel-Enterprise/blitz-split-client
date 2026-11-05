package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.NotificationPermission
import software.quare.loadable.Loadable
import software.quare.loadable.valueOrNull

// Why: "Início" (flow 10), the home's first tab and where the app opens.
data class HomeUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    val accountProvider: AccountProvider?,
    // Why: the chip, only when the sync asks for attention, as on Grupos.
    val syncBadge: SyncBadge?,
    // Why: "Offline · 1 alteração na fila": the groups, bills and payments waiting to go up.
    val pendingChangeCount: Int,
    /*
     * Why: "Sincronizando… 3 de 7 grupos" with the bar and the skeletons, while the first download of an account fills
     * a new device (after a sign in or the recovery code), rather than the first use's card; null otherwise.
     */
    val firstDownload: DownloadProgress?,
    // Why: "Ler cupom fiscal" needs the internet; everything else works from the device.
    val isOnline: Boolean,
    // Why: nothing is drawn until Room is read, so the first use's card never flashes before the groups.
    val isLoaded: Boolean,
    // Why: every group on the device, settled ones included; none and nobody is the first use.
    val groupCount: Int,
    /*
     * Why: "Saldos com pessoas": whom you owe first, then who owes you, the largest first in each. Read apart from
     * the groups, so until then it shimmers rather than reading as everyone settled.
     */
    val people: Loadable<List<HomePerson>>,
    // Why: "Saldo geral", from the same read as the people: never "Nada a receber" before it.
    val summary: Loadable<HomeSummary>,
    // Why: "Seus grupos", up to three: the pinned first, then the latest activity; settled groups stay out.
    val groups: List<HomeGroup>,
    // Why: "Últimas despesas", the five latest bills you paid or take part in, the most recent day first.
    val billDays: List<HomeBillDay>,
    // Why: "Guarde seu código de recuperação", after the first sync of an account without Google or Apple.
    val showsRecoveryCodeCard: Boolean,
    // Why: the recovery code's sheet, shown over Início once the first sync issued a code.
    val showsRecoveryCodePrompt: Boolean,
    // Why: "Pagamento de Ana registrado." with "Desfazer", once Registrar pagamento closed over Início.
    val recordedPayment: PaymentNotice?,
    // Why: "Carla apagou o grupo X.", for a group someone else deleted while Início was on top or under it.
    val deletedGroupNotice: DeletedGroupNotice?,
    // Why: a pull to refresh waits for the sync it asked for.
    val isRefreshing: Boolean,
    // Why: the count on Início's bell, every unread event of Atividade whatever its filters.
    val activityUnreadCount: Int,
    // Why: the system's, as the platform read it on appear and on each return to the app; null until then or without push.
    val notificationPermission: NotificationPermission?,
    // Why: the card was answered less than three weeks ago; true until the device's settings are read, so it never flashes.
    val isNotificationPromptSnoozed: Boolean,
) {
    val isFirstDownload: Boolean
        get() = firstDownload != null

    // Why: no group and nobody yet: only the card of 1.3, with no search, shortcuts nor "Rachar conta".
    val showsFirstUse: Boolean
        get() = isLoaded && groupCount == 0 && !isFirstDownload

    // Why: everything else Início draws, also while the first download fills it.
    val showsHome: Boolean
        get() = (isLoaded && groupCount > 0) || isFirstDownload

    /*
     * Why: "Saldos com pessoas" shimmers while the balances are read, and while the first download brought nobody yet,
     * whom the rest of the groups may still bring; the faces that came show.
     */
    val isLoadingPeople: Boolean
        get() = people.valueOrNull()?.takeIf { it.isNotEmpty() || !isFirstDownload } == null

    // Why: everyone settled: the row of faces goes without leaving a gap, and the card says "tudo acertado".
    val showsPeople: Boolean
        get() = people.valueOrNull()?.isNotEmpty() == true

    // Why: "Você deve" goes straight to settling up with them ("Acertar com Ana") while only they are owed.
    val onlyPersonToPay: HomePerson?
        get() = people.valueOrNull()?.singleOrNull { it.netMinor < 0 }

    // Why: "Rachar conta" and "Buscar" (5.2) need a group, settled ones included, to split in or look in.
    private val hasGroups: Boolean
        get() = isLoaded && groupCount > 0

    val showsSplitBillButton: Boolean
        get() = hasGroups

    val canSearch: Boolean
        get() = hasGroups

    // Why: reading a receipt needs the internet; offline it shows disabled with "Precisa de internet" (7.1d).
    val canScanReceipt: Boolean
        get() = isOnline

    val canRetrySync: Boolean
        get() = syncBadge == SyncBadge.FAILED

    /*
     * Why: the notification card, once there is a group to be notified about (one created or joined), never over the
     * first use, the first download nor the recovery code's card, and only while the system doesn't allow them.
     */
    val notificationPrompt: NotificationPrompt?
        get() {
            if (!hasGroups || isFirstDownload || showsRecoveryCodeCard || isNotificationPromptSnoozed) return null
            return when (notificationPermission) {
                NotificationPermission.NOT_DETERMINED -> NotificationPrompt.SOFT_ASK
                NotificationPermission.REFUSED_ONCE -> NotificationPrompt.ASK_AGAIN
                NotificationPermission.DENIED -> NotificationPrompt.BLOCKED
                NotificationPermission.GRANTED, null -> null
            }
        }
}
