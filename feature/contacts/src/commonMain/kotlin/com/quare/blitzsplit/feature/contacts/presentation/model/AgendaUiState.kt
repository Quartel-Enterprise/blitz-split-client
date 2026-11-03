package com.quare.blitzsplit.feature.contacts.presentation.model

import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

// Why: "Convidar da agenda" (11.3), over Contatos on a phone and in its pane in a wide window. Not on the web.
data class AgendaUiState(
    val access: AgendaAccess,
    /*
     * Why: A to Z, as the platform read them; only the ones picked under iOS's limited access. Loading until the
     * platform read them with the access it has (OnContactsLoaded), so "Na agenda · 0" never shows before.
     */
    val contacts: Loadable<List<AgendaContact>>,
    val query: String,
) {
    // Why: "Chame gente da sua agenda" before the system asks (11.3b).
    val showsAskAccess: Boolean get() = access == AgendaAccess.NOT_ASKED

    // Why: "Sem acesso à agenda", with "Abrir configurações" and the link (11.3c).
    val showsDenied: Boolean get() = access == AgendaAccess.DENIED

    val showsContacts: Boolean get() = access == AgendaAccess.GRANTED || access == AgendaAccess.LIMITED

    // Why: the access is given and the platform is still reading the agenda: the rows shimmer, with no count.
    val isLoadingContacts: Boolean get() = showsContacts && contacts.isLoading()

    // Why: "Sua agenda está vazia", only once the agenda was read.
    val showsEmptyAgenda: Boolean
        get() = showsContacts && contacts.valueOrNull()?.isEmpty() == true

    // Why: what "Na agenda · 148" lists: everyone, or who matches what was typed, with where it matched.
    val results: List<AgendaMatch> by lazy {
        val folded = Search.toFolded(query.trim())
        contacts.valueOrNull().orEmpty().mapNotNull { contact ->
            val match = if (folded.isEmpty()) NameMatch(0, 0) else Search.matchOrNull(contact.name, folded)
            match?.let { AgendaMatch(contact, Search.initialsOf(contact.name), it) }
        }
    }

    val showsNoResults: Boolean
        get() = showsContacts && contacts.isLoaded() && query.isNotBlank() && results.isEmpty()
}
