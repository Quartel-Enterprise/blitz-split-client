package com.quare.blitzsplit.core.groups.domain.model

sealed interface JoinOutcome {
    data class Joined(
        val groupId: String,
    ) : JoinOutcome

    // Why: this account had joined already, from this device or another; nothing changed.
    data class AlreadyMember(
        val groupId: String,
    ) : JoinOutcome

    // Why: someone else joined as that member first, from another device.
    data object MemberTaken : JoinOutcome

    // Why: that member was removed from the group in the meantime.
    data object MemberGone : JoinOutcome

    // Why: a member of the group already has the new name.
    data object NameTaken : JoinOutcome

    // Why: the link was changed or the group deleted.
    data object InvalidLink : JoinOutcome
}
