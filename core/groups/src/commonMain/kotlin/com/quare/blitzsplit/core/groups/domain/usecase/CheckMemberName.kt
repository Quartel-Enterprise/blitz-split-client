package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck

fun interface CheckMemberName {
    operator fun invoke(name: String): MemberNameCheck
}
