package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules

// Why: "Entrar com outro nome", for someone who isn't in the list. The name may repeat a member's (beta feedback 3).
data class NewMemberDraft(
    val name: String,
) {
    val canJoin: Boolean
        get() = MemberNameRules.isLongEnough(name)
}
