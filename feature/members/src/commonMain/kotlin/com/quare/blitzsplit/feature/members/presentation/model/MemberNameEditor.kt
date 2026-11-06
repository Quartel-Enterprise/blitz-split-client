package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules

// Why: the dialog of "Adicionar pessoa" and "Renomear", open while not null in the state.
data class MemberNameEditor(
    // Why: the member being renamed, null while adding someone.
    val memberId: String?,
    // Why: a name may repeat in the group (beta feedback 3, item 29); people are told apart by their member ids.
    val draft: String,
) {
    val isAdding: Boolean
        get() = memberId == null

    val canSave: Boolean
        get() = MemberNameRules.isLongEnough(draft)
}
