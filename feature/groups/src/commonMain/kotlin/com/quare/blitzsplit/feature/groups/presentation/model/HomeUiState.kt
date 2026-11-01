package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor

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
     * Why: "Sincronizando…" with the bar and the skeletons, while the first download of an account fills an empty
     * device (after a sign in or the recovery code), rather than the first use's card.
     */
    val isFirstDownload: Boolean,
    // Why: "Ler cupom fiscal" needs the internet; everything else works from the device.
    val isOnline: Boolean,
    // Why: nothing is drawn until Room is read, so the first use's card never flashes before the groups.
    val isLoaded: Boolean,
    // Why: every group on the device, settled ones included; none and nobody is the first use.
    val groupCount: Int,
    // Why: "Saldos com pessoas": whom you owe first, then who owes you, the largest first in each.
    val people: List<HomePerson>,
    val summary: HomeSummary,
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
) {
    // Why: no group and nobody yet: only the card of 1.3, with no search, shortcuts nor "Rachar conta".
    val showsFirstUse: Boolean
        get() = isLoaded && groupCount == 0 && !isFirstDownload

    // Why: everything else Início draws, also while the first download fills it.
    val showsHome: Boolean
        get() = (isLoaded && groupCount > 0) || isFirstDownload

    // Why: everyone settled: the row of faces goes without leaving a gap, and the card says "tudo acertado".
    val showsPeople: Boolean
        get() = people.isNotEmpty()

    // Why: "Você deve" goes straight to settling up with them ("Acertar com Ana") while only they are owed.
    val onlyPersonToPay: HomePerson?
        get() = people.singleOrNull { it.netMinor < 0 }

    val showsSplitBillButton: Boolean
        get() = isLoaded && groupCount > 0

    val canScanReceipt: Boolean
        get() = isOnline

    val canRetrySync: Boolean
        get() = syncBadge == SyncBadge.FAILED
}
