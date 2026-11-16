package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules
import com.quare.blitzsplit.core.utils.TextFolding

/*
 * Why: "Add by name" suggests the app's Contacts (feedback of 2026-11-16) whose name, or one of its words, starts with
 * what is typed, ignoring case and accents, so a group is created with that same person rather than a guest named
 * like them. Hidden ones, those already added and names too short to add are left out.
 */
object ContactSuggestions {
    const val MAX_SUGGESTIONS = 5

    fun of(
        contacts: List<Contact>,
        draft: String,
        addedKeys: Set<String>,
    ): List<CreateGroupSuggestion> {
        val query = toWords(draft)
        if (query.isEmpty()) return emptyList()
        return contacts
            .asSequence()
            .filterNot { it.isHidden || it.key in addedKeys || !MemberNameRules.isLongEnough(it.name) }
            .mapNotNull { contact ->
                val name = toWords(contact.name)
                when {
                    name.startsWith(query) -> contact to 0
                    " $name".contains(" $query") -> contact to 1
                    else -> null
                }
            }
            // Why: a name that starts with what is typed first; Contacts' A to Z order within each.
            .sortedBy { it.second }
            .take(MAX_SUGGESTIONS)
            .map { (contact, _) -> contact.toSuggestion() }
            .toList()
    }

    private fun toWords(text: String): String = TextFolding
        .toFolded(text)
        .split(' ', '\t', '\n')
        .filter { it.isNotEmpty() }
        .joinToString(" ")

    private fun Contact.toSuggestion(): CreateGroupSuggestion = CreateGroupSuggestion(
        key = key,
        name = name,
        initials = Initials.of(name),
        avatarColor = color,
        photoUrl = photoUrl,
    )
}
