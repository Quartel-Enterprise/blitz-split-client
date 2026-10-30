package com.quare.blitzsplit.feature.join.presentation.model

data class JoinPickUiState(
    // Why: nothing is drawn until the invite comes.
    val isLoaded: Boolean,
    // Why: in the order they were added, the creator first.
    val members: List<JoinMember>,
    val selectedMemberId: String?,
    // Why: "Bruno acabou de entrar por outro aparelho", once someone else joined as the picked member first.
    val takenName: String?,
    // Why: "Se era você, fale com o Pierre"; null once the creator's account was deleted.
    val creatorName: String?,
    // Why: the "Entrar com outro nome" sheet, open while not null.
    val newMember: NewMemberDraft?,
    // Why: a join on its way, which no button can start again.
    val isJoining: Boolean,
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
}
