package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactHistoryItem
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.HomeTab
import software.quare.loadable.Loadable
import software.quare.loadable.isLoading

data class ContactProfileUiState(
    // Why: null until Room is read, and once a sync took the person away (they left every group shared).
    val contact: Contact?,
    // Why: "Histórico com Carla", the latest few; "Ver tudo" opens Atividade for the rest.
    val history: List<ContactHistoryItem>,
    val hasMoreHistory: Boolean,
    val isOnline: Boolean,
    val isLoaded: Boolean,
    // Why: "Ocultar Felipe?" is open.
    val isHideConfirmOpen: Boolean,
    // Why: "Adicionar Carla a um grupo" (11.5) is open.
    val addToGroup: AddToGroupDraft?,
    // Why: "Juntar Diego com a conta dele" (11.2h) is open.
    val merge: GuestMergeSheet?,
    /*
     * Why: "Aguardando Diego aceitar", as the server last said; Loaded(null) when nothing waits. Loading until the
     * server answered for this guest, which only it knows: offline or after a failed ask it stays so, and is asked
     * again once back online.
     */
    val pendingMerge: Loadable<GuestMerge?>,
    // Why: the last ask for the waiting request failed online; it is asked again once the connection comes back.
    val hasMergeStatusFailed: Boolean,
    // Why: Cancelar was tapped and the server hasn't answered yet.
    val isCancellingMerge: Boolean,
    /*
     * Why: the home tab the profile was opened from (Contatos, or Saldos for a face tapped there), which the web's frame
     * selects and its back names.
     */
    val homeTab: HomeTab,
    // Why: right above that tab, back returns to it and the web's back names it; over a bill or a group it says "Voltar".
    val isOverHomeTab: Boolean,
) {
    val initials: String
        get() = contact?.name?.let(Initials::of).orEmpty()

    private val netMinor: Long
        get() = contact?.net?.amountMinor ?: 0L

    // Why: te deve → Cobrar and Recebi; você deve → Acertar; zero → only Adicionar a um grupo, which every sign has.
    val canCharge: Boolean
        get() = netMinor > 0

    val canReceive: Boolean
        get() = netMinor > 0

    val canSettle: Boolean
        get() = netMinor < 0

    // Why: "Ocultar de Contatos" only at zero; otherwise disabled with "Só com saldo zero. Acerte antes."
    val canHide: Boolean
        get() = contact?.isSettled == true && !contact.isHidden

    // Why: someone hidden whose profile was opened from the search or Ocultos: the menu offers "Mostrar em Contatos".
    val canShow: Boolean
        get() = contact?.isHidden == true

    private val knownPendingMerge: GuestMerge?
        get() = (pendingMerge as? Loadable.Loaded)?.value

    /*
     * Why: "Diego ainda não usa o Bora Rachar", with Juntar and Convidar, unless a request is known to wait for him;
     * while the server hasn't answered, the merge row draws isMergeStatusLoading or isMergeStatusUnavailable instead.
     */
    val showsGuestCard: Boolean
        get() = contact?.isGuest == true && knownPendingMerge == null

    val showsPendingMerge: Boolean
        get() = contact?.isGuest == true && knownPendingMerge != null

    // Why: online and the server not answered yet: the merge row shimmers rather than offering Juntar.
    val isMergeStatusLoading: Boolean
        get() = contact?.isGuest == true && pendingMerge.isLoading() && isOnline && !hasMergeStatusFailed

    /*
     * Why: offline (or the ask failed) before the server ever answered: whether a request waits is unknown, so the
     * row claims neither Juntar nor "Aguardando"; it says the merge needs the internet.
     */
    val isMergeStatusUnavailable: Boolean
        get() = contact?.isGuest == true && pendingMerge.isLoading() && (!isOnline || hasMergeStatusFailed)

    // Why: "Juntar com a conta dele" in the menu, disabled offline with "Precisa de internet", and until it is known.
    val canMerge: Boolean
        get() = contact?.isGuest == true && isOnline && !pendingMerge.isLoading()

    // Why: "Já compensado entre 3 grupos" under the net; one group says its name.
    val isNetOfSeveralGroups: Boolean
        get() = (contact?.groups?.distinctBy { it.groupId }?.size ?: 0) > 1

    val balanceNote: ContactBalanceNote
        get() = when {
            contact == null || contact.groups.all { it.amountMinor == 0L } -> ContactBalanceNote.NOTHING_OPEN
            isNetOfSeveralGroups -> ContactBalanceNote.OFFSET_ACROSS_GROUPS
            else -> ContactBalanceNote.ONE_GROUP
        }

    // Why: "Offline · o que você fizer entra na fila".
    val showsOfflineNotice: Boolean
        get() = !isOnline
}
