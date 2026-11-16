package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.model.AvatarColor

data class CreateGroupMember(
    // Why: a key for the list and for "Remover", made on the device; the group gets its own ids once created.
    val id: String,
    val name: String,
    // Why: "A" for Ana, "AP" for Ana Paula; this device's user shows the single initial of the home's avatar.
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: the profile photo over the initials, this device's user's as the home's avatar shows it; null for none.
    val photoUrl: String?,
    // Why: "você", and the one member that can't be removed.
    val isMe: Boolean,
    // Why: the account of someone picked from Contacts, who joins as it; null for a name typed and for "você".
    val userId: String?,
) {
    /*
     * Why: Contacts' person key (PeopleBalances.keyOf), so a suggestion never offers someone already added; null for
     * "você", whom Contacts never lists.
     */
    val personKey: String?
        get() = if (isMe) null else userId?.let(PeopleBalances::toUserKey) ?: PeopleBalances.toNameKey(name)
}
