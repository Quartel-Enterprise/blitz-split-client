package com.quare.blitzsplit.core.groups.domain.model

sealed interface MemberNameCheck {
    // Why: the name as it goes into the group, trimmed and with single spaces.
    data class Available(
        val name: String,
    ) : MemberNameCheck

    data object Blank : MemberNameCheck

    // Why: a single letter, shorter than MemberNameRules allows.
    data object TooShort : MemberNameCheck
}
