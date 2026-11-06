package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName

/*
 * Why: two members of a group may have the same name (beta feedback 3, item 29): people are told apart by their
 * member and user ids, not their name. So a name only needs its letters, trimmed and with single spaces.
 */
class CheckMemberNameUseCase : CheckMemberName {
    private val whitespace = Regex("\\s+")

    override fun invoke(name: String): MemberNameCheck {
        val normalized = name.trim().split(whitespace).joinToString(" ")
        return when {
            normalized.isEmpty() -> MemberNameCheck.Blank
            !MemberNameRules.isLongEnough(normalized) -> MemberNameCheck.TooShort
            else -> MemberNameCheck.Available(normalized)
        }
    }
}
