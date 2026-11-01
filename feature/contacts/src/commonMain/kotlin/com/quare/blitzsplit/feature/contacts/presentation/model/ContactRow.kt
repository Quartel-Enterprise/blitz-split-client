package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.model.AvatarColor

// Why: one person in Contatos, A to Z: their net across the groups, and where they are.
data class ContactRow(
    val key: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    // Why: "+R$ 32,90 te deve", "−R$ 126,00 você deve" or "R$ 0,00 · quitado".
    val net: ContactNet,
    // Why: "3 grupos em comum" for several, the group's name for one.
    val groupCount: Int,
    val firstGroupName: String,
    // Why: "· convidado sem app".
    val isGuest: Boolean,
    // Why: the search marks where the name matched ("Ca rla"); null outside the search.
    val match: NameMatch?,
)
