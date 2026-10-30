package com.quare.blitzsplit.feature.members.presentation.model

data class MembersUiState(
    val groupName: String,
    // Why: "blitzsplit.com/g/7Hk2pQ", null until the group's first sync brings the code the server made.
    val displayLink: String?,
    // Why: in the order they were added, the creator first.
    val members: List<MembersMember>,
    val editor: MemberNameEditor?,
    // Why: "Remover Carla do grupo?", asked while not null; always someone who can be removed.
    val memberToRemove: MembersMember?,
    // Why: the "Convite pessoal" sheet, open while not null; always someone nobody joined as, with a link.
    val personalInvite: PersonalInvite?,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    // Why: "Você entrou como Bruno", right after joining the group through its link.
    val joinedAs: String?,
    // Why: "Proteja seu acesso" under it, while the account has no Google or Apple.
    val isAccountUnprotected: Boolean,
) {
    val showsProtectAccess: Boolean
        get() = joinedAs != null && isAccountUnprotected

    // Why: "4 membros · 2 já entraram".
    val memberCount: Int
        get() = members.size

    val joinedCount: Int
        get() = members.count { it.hasJoined }
}
