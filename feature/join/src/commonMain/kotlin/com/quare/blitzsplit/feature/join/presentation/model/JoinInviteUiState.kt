package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class JoinInviteUiState(
    val status: JoinInviteStatus,
    val groupName: String,
    // Why: "VF" for Viagem Floripa.
    val groupInitials: String,
    val groupColor: AvatarColor,
    // Why: "Pierre te chamou", "criado por Pierre"; null once the creator's account was deleted.
    val creatorName: String?,
    // Why: in the order they were added, the creator first.
    val members: List<JoinMember>,
    // Why: "blitzsplit.com/g/7Hk2pQ", the link as the offline state shows it.
    val displayLink: String,
    // Why: a personal link, which joins as one member ("Convite pessoal"), known before the invite comes.
    val isPersonal: Boolean,
    // Why: "Pierre te chamou como Carla": the member a personal invite joins as, once the invite came.
    val personalMember: JoinMember?,
    /*
     * Why: "Ana te chamou como Carla", whoever shared a personal link; null when the link doesn't say, rather than
     * naming the creator for someone else's link: "Você entra como Carla".
     */
    val inviterName: String?,
    // Why: "Entrar como Carla" on its way, which no button can start again.
    val isJoining: Boolean,
) {
    // Why: "4 membros".
    val memberCount: Int
        get() = members.size

    // Why: the faces next to "Pierre e Ana já estão no grupo".
    val joined: List<JoinMember>
        get() = members.filter { it.hasJoined }

    // Why: "Pierre e Ana já estão no grupo".
    val joinedNames: List<String>
        get() = joined.map { it.name }
}
