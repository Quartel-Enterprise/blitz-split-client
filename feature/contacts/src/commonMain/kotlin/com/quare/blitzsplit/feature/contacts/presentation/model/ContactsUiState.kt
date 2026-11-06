package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import software.quare.loadable.Loadable

data class ContactsUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: the web's sidebar names the account under the profile; null without Google or Apple.
    val accountProvider: AccountProvider?,
    // Why: everyone the user shares a group with and hasn't hidden, A to Z.
    val contacts: List<ContactRow>,
    // Why: "Ocultos · 1", at the end of the list, each with "Mostrar".
    val hiddenContacts: List<ContactRow>,
    val isHiddenExpanded: Boolean,
    /*
     * Why: "Você deve R$ 126,00 · Te devem R$ 312,40", the same totals as Saldos, in its main currency. Read apart from
     * the people, and Loading too while the account's copy is not whole, so it shimmers rather than saying R$ 0,00.
     */
    val total: Loadable<PeopleTotal>,
    val isOnline: Boolean,
    /*
     * Why: nothing is drawn until Room is read and the account's copy on this device is whole: who was hidden on
     * another device only comes with its full sync, so until then the empty state and the rows never flash.
     */
    val isLoaded: Boolean,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the skeleton while the copy waits offline.
    val isWaitingForInternet: Boolean,
    // Why: the UI draws Contatos with the profile beside it (a wide window), so a tap picks instead of opening.
    val isDetailPaneShown: Boolean,
    // Why: what the pane beside the list shows: a person, the agenda, or nothing yet (it says what will show there).
    val pickedKey: String?,
    val isAgendaPicked: Boolean,
    // Why: "Buscar" (5.2), the same search as Grupos', only once there is a group to look in.
    val canSearch: Boolean,
    // Why: Android's "Rachar conta", once there is a group to split in, as on Início.
    val showsSplitBillButton: Boolean,
) {
    // Why: a profile whose person a sync took away (they left every group shared) leaves the pane to its placeholder.
    val selectedKey: String?
        get() = pickedKey?.takeIf { key -> (contacts + hiddenContacts).any { it.key == key } }

    // Why: "Ninguém por aqui ainda": nobody shares a group with the user yet, hidden ones aside.
    val isEmpty: Boolean
        get() = isLoaded && contacts.isEmpty() && hiddenContacts.isEmpty()

    // Why: the skeleton rows, only until Room is read.
    val isLoading: Boolean
        get() = !isLoaded

    // Why: the card that leads to Saldos; not with nobody to count.
    val showsSummary: Boolean
        get() = isLoaded && !isEmpty

    val showsOfflineNotice: Boolean
        get() = !isOnline
}
