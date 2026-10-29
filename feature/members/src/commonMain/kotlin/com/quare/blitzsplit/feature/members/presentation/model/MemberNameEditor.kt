package com.quare.blitzsplit.feature.members.presentation.model

// Why: the dialog of "Adicionar pessoa" and "Renomear", open while not null in the state.
data class MemberNameEditor(
    // Why: the member being renamed, null while adding someone.
    val memberId: String?,
    val draft: String,
    // Why: "Já tem uma Ana no grupo", as the group spells it, while the draft matches another member.
    val takenName: String?,
    // Why: "Ana P." and "Ana 2", free in the group, to tap instead.
    val nicknameSuggestions: List<String>,
) {
    val isAdding: Boolean
        get() = memberId == null

    val canSave: Boolean
        get() = draft.isNotBlank() && takenName == null
}
