package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck

fun interface CheckMemberName {
    // Why: adderName, whoever is adding the name, gives the nickname its initial ("Ana P." when Pierre adds Ana).
    operator fun invoke(
        name: String,
        takenNames: List<String>,
        adderName: String,
    ): MemberNameCheck
}
