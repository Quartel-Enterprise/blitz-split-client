package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactHistoryItem
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.groups.domain.model.Initials

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
    // Why: "Aguardando Diego aceitar", as the server last said; null offline or when nothing waits.
    val pendingMerge: GuestMerge?,
    // Why: Cancelar was tapped and the server hasn't answered yet.
    val isCancellingMerge: Boolean,
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

    // Why: "Diego ainda não usa o Bora Rachar", with Juntar and Convidar, until a request waits for him.
    val showsGuestCard: Boolean
        get() = contact?.isGuest == true && pendingMerge == null

    val showsPendingMerge: Boolean
        get() = contact?.isGuest == true && pendingMerge != null

    // Why: "Juntar com a conta dele" in the menu, disabled offline with "Precisa de internet".
    val canMerge: Boolean
        get() = contact?.isGuest == true && isOnline

    // Why: "Já compensado entre 3 grupos" under the net; one group says its name.
    val isNetOfSeveralGroups: Boolean
        get() = (contact?.groups?.size ?: 0) > 1

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
