package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor

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
    // Why: "Você deve R$ 126,00 · Te devem R$ 312,40", the same totals as Saldos, in its main currency.
    val total: PeopleTotal,
    // Why: "Buscar contatos" is open (Android's and iOS's search screen, the web's and the Mac's field).
    val isSearchOpen: Boolean,
    val query: String,
    // Why: the device's agenda, while the platform may read it; the search looks in it after the groups.
    val agenda: List<AgendaContact>,
    val isOnline: Boolean,
    // Why: nothing is drawn until Room is read, so the empty state never flashes.
    val isLoaded: Boolean,
    // Why: the UI draws Contatos with the profile beside it (a wide window), so a tap picks instead of opening.
    val isDetailPaneShown: Boolean,
    // Why: what the pane beside the list shows: a person, the agenda, or nothing yet (it says what will show there).
    val pickedKey: String?,
    val isAgendaPicked: Boolean,
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

    // Why: the card that leads to Saldos; not while searching, nor with nobody to count.
    val showsSummary: Boolean
        get() = isLoaded && !isEmpty && !isSearching

    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: the search shows its results in place of the list once something is typed.
    val isSearching: Boolean
        get() = isSearchOpen && query.isNotBlank()

    // Why: "Nos seus grupos", hidden ones included, A to Z.
    val groupResults: List<ContactRow> by lazy {
        val folded = Search.toFolded(query.trim())
        if (folded.isEmpty()) return@lazy emptyList()
        (contacts + hiddenContacts)
            .sortedBy { Search.toFolded(it.name) }
            .mapNotNull { row -> Search.matchOrNull(row.name, folded)?.let { row.copy(match = it) } }
    }

    // Why: "Na agenda", after the groups, without whoever already shows there by the same name.
    val agendaResults: List<AgendaMatch> by lazy {
        val folded = Search.toFolded(query.trim())
        if (folded.isEmpty()) return@lazy emptyList()
        val inGroups = groupResults.map { Search.toFolded(it.name) }.toSet()
        agenda
            .filterNot { Search.toFolded(it.name) in inGroups }
            .mapNotNull { contact ->
                Search.matchOrNull(contact.name, folded)?.let { match ->
                    AgendaMatch(
                        contact = contact,
                        initials = Search.initialsOf(contact.name),
                        match = match,
                    )
                }
            }
    }

    // Why: "Ninguém com “Rodrigo”", with the link.
    val showsNoResults: Boolean
        get() = isSearching && groupResults.isEmpty() && agendaResults.isEmpty()
}
