package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules

// Why: "Entrar com outro nome", for someone who isn't in the list.
data class NewMemberDraft(
    val name: String,
    // Why: "Não pode repetir um nome que já está no grupo", as an error.
    val isNameTaken: Boolean,
) {
    val canJoin: Boolean
        get() = MemberNameRules.isLongEnough(name) && !isNameTaken
}
