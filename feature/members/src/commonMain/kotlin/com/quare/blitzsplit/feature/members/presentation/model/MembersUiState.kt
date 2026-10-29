package com.quare.blitzsplit.feature.members.presentation.model

data class MembersUiState(
    val groupName: String,
    // Why: "borarachar.com/g/7Hk2pQ", null until the group's first sync brings the code the server made.
    val displayLink: String?,
    // Why: in the order they were added, the creator first.
    val members: List<MembersMember>,
    val editor: MemberNameEditor?,
    // Why: "Remover Carla do grupo?", asked while not null; always someone who can be removed.
    val memberToRemove: MembersMember?,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    // Why: "4 membros · 2 já entraram".
    val memberCount: Int
        get() = members.size

    val joinedCount: Int
        get() = members.count { it.hasJoined }
}
