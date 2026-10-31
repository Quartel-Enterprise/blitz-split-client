package com.quare.blitzsplit.feature.invite.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.model.QrCode

data class InviteUiState(
    val groupName: String,
    // Why: null until the group's first sync brings the code the server made, offline right after creating it.
    val link: String?,
    val qrCode: QrCode?,
    // Why: in the order they were added, the creator first.
    val members: List<InviteMember>,
    // Why: a pending link says it comes once online, or that it is on its way.
    val isOnline: Boolean,
    // Why: the last sync failed online, so a pending link won't come by itself; "Tentar de novo" syncs again.
    val isSyncFailed: Boolean,
    // Why: "Copiado" for two seconds after "Copiar".
    val isLinkCopied: Boolean,
    // Why: nothing is drawn until Room is read, so a link already there never flashes as pending.
    val isLoaded: Boolean,
    // Why: only the group's creator may change its link.
    val isCreatedByMe: Boolean,
    // Why: right after "Novo grupo", which "Grupo criado" says; "Trocar link" is only offered from the group.
    val isNewGroup: Boolean,
    // Why: "Trocar o link do convite?" is open.
    val isConfirmingLinkChange: Boolean,
    // Why: the server is making the new link; the old one stays on screen until it comes.
    val isChangingLink: Boolean,
) {
    val isLinkPending: Boolean
        get() = isLoaded && link == null

    // Why: "Trocar link", for the creator, from the group and online, once there is a link to change.
    val showsChangeLink: Boolean
        get() = isLoaded && isCreatedByMe && !isNewGroup && isOnline && link != null

    // Why: "Não deu para criar o link", in place of the pending copy, with a retry.
    val isLinkFailed: Boolean
        get() = isLinkPending && isOnline && isSyncFailed

    // Why: "blitzsplit.com/g/7Hk2pQ", the link as the card shows it; copying and sharing send the whole link.
    val displayLink: String?
        get() = link?.let(InviteLink::displayOf)

    // Why: "Quem abrir o link escolhe quem é na lista: Ana, Bruno ou Carla".
    val notJoinedNames: List<String>
        get() = members.filterNot { it.hasJoined }.map { it.name }
}
