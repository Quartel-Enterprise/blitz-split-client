package com.quare.blitzsplit.feature.join.presentation.model

data class JoinPickUiState(
    // Why: nothing is drawn until the invite comes.
    val isLoaded: Boolean,
    // Why: "Entre na Viagem Floripa" beside the names on the web's desktop.
    val groupName: String,
    // Why: in the order they were added, the creator first.
    val members: List<JoinMember>,
    val selectedMemberId: String?,
    // Why: "Bruno acabou de entrar por outro aparelho", once someone else joined as the picked member first.
    val takenName: String?,
    // Why: "Se era você, peça para Pierre liberar o nome em Membros"; null once the creator's account was deleted.
    val creatorName: String?,
    // Why: the "Entrar com outro nome" sheet, open while not null.
    val newMember: NewMemberDraft?,
    // Why: a join on its way, which no button can start again.
    val isJoining: Boolean,
    /*
     * Why: the join on its way is a new member's, from "Entrar com outro nome" or, for a Google or Apple account,
     * straight from "Não estou na lista": its button shows the progress rather than "Entrar como Bruno".
     */
    val isJoiningAsNewMember: Boolean,
) {
    // Why: "Ainda não entraram", the ones that can be picked.
    val notJoined: List<JoinMember>
        get() = members.filterNot { it.hasJoined }

    // Why: "Já estão no grupo", shown but not picked.
    val joined: List<JoinMember>
        get() = members.filter { it.hasJoined }

    // Why: "Entrar como Bruno"; with nobody picked the button reads "Escolha um nome" and stays off.
    val selectedName: String?
        get() = members.firstOrNull { it.id == selectedMemberId }?.name

    val canJoin: Boolean
        get() = selectedName != null && !isJoining

    val canJoinAsNewMember: Boolean
        get() = newMember?.canJoin == true && !isJoining

    // Why: "Entrar como Bruno" spins only for the member picked, not for a new member's join.
    val showsJoinProgress: Boolean
        get() = isJoining && !isJoiningAsNewMember

    // Why: "Não estou na lista" spins while a Google or Apple account joins from it at once, with no sheet open.
    val showsNotInListProgress: Boolean
        get() = isJoiningAsNewMember && newMember == null
}
