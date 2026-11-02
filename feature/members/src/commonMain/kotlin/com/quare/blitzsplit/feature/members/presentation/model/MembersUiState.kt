package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: Membros, under the group's header.
data class MembersUiState(
    val groupName: String,
    val currency: GroupCurrency,
    // Why: "blitzsplit.com/g/7Hk2pQ", null until the group's first sync brings the code the server made.
    val displayLink: String?,
    // Why: in the order they were added, the creator first.
    val members: List<MembersMember>,
    val editor: MemberNameEditor?,
    // Why: "Remover Carla do grupo?", asked while not null; always someone who can be removed.
    val memberToRemove: MembersMember?,
    // Why: "Liberar o nome Carla?", asked while not null; always someone who can be released.
    val memberToRelease: MembersMember?,
    // Why: the release goes to the server, so "Liberar" waits for it with the confirmation open.
    val isReleasing: Boolean,
    // Why: the "Convite pessoal" sheet, open while not null; always someone nobody joined as, with a link.
    val personalInvite: PersonalInvite?,
    // Why: nothing is drawn under the group's header until Room is read.
    val isLoaded: Boolean,
)
