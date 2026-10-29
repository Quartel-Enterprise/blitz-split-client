package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck

fun interface CheckMemberName {
    // Why: creatorName gives the nickname its initial ("Ana P." in Pierre's group).
    operator fun invoke(
        name: String,
        takenNames: List<String>,
        creatorName: String,
    ): MemberNameCheck
}
