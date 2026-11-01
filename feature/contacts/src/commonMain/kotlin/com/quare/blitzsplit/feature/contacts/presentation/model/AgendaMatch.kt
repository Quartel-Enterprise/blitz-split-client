package com.quare.blitzsplit.feature.contacts.presentation.model

// Why: "Na agenda", a contact the search found, with where it matched.
data class AgendaMatch(
    val contact: AgendaContact,
    val initials: String,
    val match: NameMatch,
)
